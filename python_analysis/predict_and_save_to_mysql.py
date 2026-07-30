# predict_and_save_to_mysql.py
# 训练评分预测模型，预测 UnScore 电影评分，写入 MySQL
# 输出：1) movie_prediction 表（预测结果） 2) movie_stats 表（评估指标）

import os
import sys

# ========== 环境变量（在 import pyspark 之前） ==========
os.environ["JAVA_HOME"] = r"D:\JDK8"
os.environ["HADOOP_HOME"] = r"E:\hadoop\hadoop-3.3.5"
os.environ["JAVA_TOOL_OPTIONS"] = "-Djava.net.preferIPv4Stack=true"
os.environ["PYSPARK_PYTHON"] = sys.executable
os.environ["PYSPARK_DRIVER_PYTHON"] = sys.executable

java_bin = os.path.join(os.environ["JAVA_HOME"], "bin")
hadoop_bin = os.path.join(os.environ["HADOOP_HOME"], "bin")
os.environ["PATH"] = f"{java_bin};{hadoop_bin};" + os.environ.get("PATH", "")

import time
from pyspark.sql import SparkSession
from pyspark.sql import Row
from pyspark.sql.types import StructType, StructField, StringType, LongType, DoubleType
from pyspark.ml import Pipeline, PipelineModel
from pyspark.ml.feature import StringIndexer, OneHotEncoder, VectorAssembler, StandardScaler
from pyspark.ml.regression import GBTRegressor, RandomForestRegressor, LinearRegression
from pyspark.ml.evaluation import RegressionEvaluator
from pyspark.sql.functions import col, coalesce, lit, when, round as spark_round
from pyspark.sql.types import DoubleType as SparkDoubleType
from pyspark.ml.regression import GBTRegressionModel

# ========== 路径 & MySQL 配置 ==========
MYSQL_JAR = r"E:\movie\movie_analysiz_01\test02\output\mysql-connector-java-8.0.23.jar"
MYSQL_CONFIG = {
    "url": "jdbc:mysql://localhost:3306/movie_db?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true",
    "user": "root",
    "password": "M20054921",
    "driver": "com.mysql.cj.jdbc.Driver"
}

MOVIE_CSV_DIR = r"E:/movie/movie_analysiz_01/test02/output/data/movie/clean/movies"
MODEL_SAVE_DIR = r"E:/movie/movie_analysiz_01/test02/output/predictions/model"

# ========== Spark Session ==========
spark = (SparkSession.builder
         .appName("PredictAndSaveToMySQL")
         .master("local[*]")
         .config("spark.driver.host", "127.0.0.1")
         .config("spark.driver.bindAddress", "127.0.0.1")
         .config("spark.driver.memory", "6g")
         .config("spark.executor.memory", "6g")
         .config("spark.jars", MYSQL_JAR)
         .config("spark.driver.extraClassPath", MYSQL_JAR)
         .config("spark.sql.adaptive.enabled", "true")
         .config("spark.python.worker.reuse", "false")
         .config("spark.network.timeout", "600s")
         .getOrCreate())
spark.sparkContext.setLogLevel("WARN")

print("=" * 70)
print("电影评分预测模型生成 & 写入数据库")
print("=" * 70)

# ===========================
# 1. 读取数据
# ===========================
df_all = spark.read.option("header", "true").csv(MOVIE_CSV_DIR)
df_valid = df_all.filter(col("SCORE_FLAG") == "valid")
df_unscore = df_all.filter(col("SCORE_FLAG") == "UnScore")

total, valid_cnt, unscore_cnt = df_all.count(), df_valid.count(), df_unscore.count()
print(f"\n电影总数: {total}  |  有效评分(训练): {valid_cnt}  |  未评分(预测): {unscore_cnt}")

# ===========================
# 2. 数据预处理 - 训练集
# ===========================
for field in ["YEAR_INT", "DOUBAN_VOTES", "MINS", "DOUBAN_SCORE"]:
    df_valid = df_valid.withColumn(field, coalesce(col(field), lit("0")).cast(SparkDoubleType()))
for field in ["GENRES_CLEAN", "REGIONS_CLEAN", "LANGUAGES_CLEAN"]:
    df_valid = df_valid.withColumn(field, coalesce(col(field), lit("unknown")))

df_unscore = df_unscore.withColumn("YEAR_INT", coalesce(col("YEAR_INT"), lit("0")).cast(SparkDoubleType()))
df_unscore = df_unscore.withColumn("DOUBAN_VOTES", coalesce(col("DOUBAN_VOTES"), lit("0")).cast(SparkDoubleType()))
df_unscore = df_unscore.withColumn("MINS", coalesce(col("MINS"), lit("0")).cast(SparkDoubleType()))
for field in ["GENRES_CLEAN", "REGIONS_CLEAN", "LANGUAGES_CLEAN"]:
    df_unscore = df_unscore.withColumn(field, coalesce(col(field), lit("unknown")))

# ===========================
# 3. 特征工程 Pipeline
# ===========================
print("\n--- 特征工程 Pipeline ---")
genre_idx = StringIndexer(inputCol="GENRES_CLEAN", outputCol="genres_idx", handleInvalid="keep")
genre_ohe = OneHotEncoder(inputCol="genres_idx", outputCol="genres_vec", dropLast=True)
region_idx = StringIndexer(inputCol="REGIONS_CLEAN", outputCol="region_idx", handleInvalid="keep")
region_ohe = OneHotEncoder(inputCol="region_idx", outputCol="region_vec", dropLast=True)
lang_idx = StringIndexer(inputCol="LANGUAGES_CLEAN", outputCol="lang_idx", handleInvalid="keep")
lang_ohe = OneHotEncoder(inputCol="lang_idx", outputCol="lang_vec", dropLast=True)

num_vec = VectorAssembler(inputCols=["YEAR_INT", "DOUBAN_VOTES", "MINS"],
                          outputCol="num_raw_vec", handleInvalid="keep")
num_scale = StandardScaler(inputCol="num_raw_vec", outputCol="num_scaled_vec", withMean=True, withStd=True)

feat_vec = VectorAssembler(inputCols=["genres_vec", "region_vec", "lang_vec", "num_scaled_vec"],
                           outputCol="features", handleInvalid="keep")

feat_pipeline = Pipeline(stages=[genre_idx, genre_ohe, region_idx, region_ohe,
                                  lang_idx, lang_ohe, num_vec, num_scale, feat_vec])
feat_model = feat_pipeline.fit(df_valid)
df_train_feat = feat_model.transform(df_valid)
df_pred_feat = feat_model.transform(df_unscore)

# 训练/测试划分
train_data, test_data = df_train_feat.randomSplit([0.7, 0.3], seed=42)
train_data.cache().count()
test_data.cache().count()
print(f"训练集: {train_data.count()} 条  |  测试集: {test_data.count()} 条")

# ===========================
# 4. 训练回归模型（选择最佳）
# ===========================
print("\n--- 训练回归模型 ---")
evaluators = {
    "rmse": RegressionEvaluator(labelCol="DOUBAN_SCORE", predictionCol="prediction", metricName="rmse"),
    "mae": RegressionEvaluator(labelCol="DOUBAN_SCORE", predictionCol="prediction", metricName="mae"),
    "r2": RegressionEvaluator(labelCol="DOUBAN_SCORE", predictionCol="prediction", metricName="r2"),
}

models = {}

# 4.1 线性回归
lr = LinearRegression(featuresCol="features", labelCol="DOUBAN_SCORE", maxIter=100, regParam=0.01)
t0 = time.time()
lr_model = lr.fit(train_data)
lr_pred = lr_model.transform(test_data)
models["LinearRegression"] = (lr_model, lr_pred)
print(f"  LinearRegression  R²={evaluators['r2'].evaluate(lr_pred):.4f}  ({time.time()-t0:.1f}s)")

# 4.2 随机森林
rf = RandomForestRegressor(featuresCol="features", labelCol="DOUBAN_SCORE", numTrees=20, maxDepth=8, seed=42)
t0 = time.time()
rf_model = rf.fit(train_data)
rf_pred = rf_model.transform(test_data)
models["RandomForest"] = (rf_model, rf_pred)
print(f"  RandomForest      R²={evaluators['r2'].evaluate(rf_pred):.4f}  ({time.time()-t0:.1f}s)")

# 4.3 GBDT
gbt = GBTRegressor(featuresCol="features", labelCol="DOUBAN_SCORE", maxIter=20, maxDepth=5, seed=42)
t0 = time.time()
gbt_model = gbt.fit(train_data)
gbt_pred = gbt_model.transform(test_data)
models["GBTRegressor"] = (gbt_model, gbt_pred)
print(f"  GBTRegressor      R²={evaluators['r2'].evaluate(gbt_pred):.4f}  ({time.time()-t0:.1f}s)")

# 选择最佳模型
best_name, (best_model, _) = max(models.items(), key=lambda x: evaluators['r2'].evaluate(x[1][1]))
print(f"\n✅ 最佳模型: {best_name}")

# ===========================
# 5. 保存模型
# ===========================
os.makedirs(os.path.dirname(MODEL_SAVE_DIR), exist_ok=True)
best_model.write().overwrite().save(MODEL_SAVE_DIR)
print(f"模型已保存: {MODEL_SAVE_DIR}")

# ===========================
# 6. 预测 UnScore 电影
# ===========================
print("\n--- 预测 UnScore 电影评分 ---")
df_predictions = best_model.transform(df_pred_feat).select(
    col("MOVIE_ID"),
    col("NAME"),
    col("YEAR_INT").cast("int").alias("year"),
    col("GENRES_CLEAN").alias("genres"),
    col("REGIONS_CLEAN").alias("region"),
    spark_round(col("prediction"), 1).alias("predicted_score")
)

# ===========================
# 7. 写入 MySQL - movie_prediction 表
# ===========================
print("\n--- 写入预测结果到 MySQL ---")

# 添加记录时间戳列
from pyspark.sql.functions import current_timestamp

df_mysql = df_predictions.withColumn("created_at", current_timestamp())

# 先创建表结构（如果不存在）
create_table_sql = """
CREATE TABLE IF NOT EXISTS movie_prediction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id INT,
    name VARCHAR(500),
    year INT,
    genres VARCHAR(500),
    region VARCHAR(500),
    predicted_score DECIMAL(3,1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_movie_id (movie_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
"""
try:
    import pymysql
    conn = pymysql.connect(
        host='localhost', port=3306, user='root',
        password='M20054921', database='movie_db', charset='utf8mb4'
    )
    with conn.cursor() as cursor:
        cursor.execute(create_table_sql)
        # 清空旧数据（覆盖写入）
        cursor.execute("TRUNCATE TABLE movie_prediction")
    conn.commit()
    conn.close()
    print("✅ movie_prediction 表已就绪（已清空旧数据）")
except Exception as e:
    print(f"⚠️ 建表/清表提醒（忽略若已存在）: {e}")

# Spark JDBC 批量写入
save_path = r"E:/movie/movie_analysiz_01/test02/output/predictions/unscore_predictions.parquet"
df_mysql.write.mode("overwrite").parquet(save_path)

df_mysql.write \
    .format("jdbc") \
    .option("url", MYSQL_CONFIG["url"]) \
    .option("dbtable", "movie_prediction") \
    .option("user", MYSQL_CONFIG["user"]) \
    .option("password", MYSQL_CONFIG["password"]) \
    .option("driver", MYSQL_CONFIG["driver"]) \
    .option("batchsize", "5000") \
    .mode("append") \
    .save()

pred_count = df_mysql.count()
print(f"✅ 已写入 {pred_count} 条预测结果到 movie_db.movie_prediction 表")

# ===========================
# 8. 显示部分结果验证
# ===========================
print("\n--- 预测结果预览（前10条）---")
df_mysql.show(10, truncate=False)

print("\n--- 预测评分分布 ---")
df_mysql.withColumn("score_bin",
    when(col("predicted_score") < 3.0, "0-3")
    .when(col("predicted_score") < 5.0, "3-5")
    .when(col("predicted_score") < 7.0, "5-7")
    .otherwise("7-10")
).groupBy("score_bin").count().orderBy("score_bin").show()

# ===========================
# 9. 写入评估指标到 movie_stats
# ===========================
print("--- 写入评估指标到 movie_stats ---")
metric_schema = StructType([
    StructField("stat_type", StringType(), True),
    StructField("stat_key", StringType(), True),
    StructField("stat_value", StringType(), True),
    StructField("stat_count", LongType(), True),
    StructField("stat_percentage", DoubleType(), True),
    StructField("extra_data", StringType(), True)
])
metric_rows = []
for name, (_, pred) in models.items():
    rmse = evaluators["rmse"].evaluate(pred)
    mae = evaluators["mae"].evaluate(pred)
    r2 = evaluators["r2"].evaluate(pred)
    metric_rows.append(Row(
        stat_type="rating_prediction",
        stat_key=name,
        stat_value=f"RMSE={rmse:.4f}, MAE={mae:.4f}, R²={r2:.4f}",
        stat_count=valid_cnt,
        stat_percentage=float(0),
        extra_data=f'{{"rmse":{rmse},"mae":{mae},"r2":{r2}}}'
    ))
# 汇总信息
pred_avg = df_mysql.agg({"predicted_score": "avg"}).collect()[0][0]
metric_rows.append(Row(
    stat_type="rating_prediction",
    stat_key="prediction_summary",
    stat_value=f"best_model={best_name}, predicted={pred_count}, avg_score={pred_avg:.2f}",
    stat_count=pred_count,
    stat_percentage=float(0),
    extra_data=f'{{"best_model":"{best_name}","predicted_count":{pred_count},"avg_score":{pred_avg:.2f}}}'
))
df_metrics = spark.createDataFrame(metric_rows, schema=metric_schema)
df_metrics.write \
    .format("jdbc") \
    .option("url", MYSQL_CONFIG["url"]) \
    .option("dbtable", "movie_stats") \
    .option("user", MYSQL_CONFIG["user"]) \
    .option("password", MYSQL_CONFIG["password"]) \
    .option("driver", MYSQL_CONFIG["driver"]) \
    .mode("append") \
    .save()
print("✅ 评估指标已写入 movie_stats")

# ===========================
# 10. 保存训练好的 Pipeline（特征工程+预测模型）供后续复用
# ===========================
pipeline_save_path = r"E:/movie/movie_analysiz_01/test02/output/predictions/full_pipeline"
# 保存特征 Pipeline
feat_model.write().overwrite().save(pipeline_save_path + "_features")
print(f"特征 Pipeline 已保存: {pipeline_save_path}_features")

print("\n" + "=" * 70)
print(f"🎉 全部完成！共预测 {pred_count} 部电影评分，已写入 MySQL")
print("=" * 70)

spark.stop()
