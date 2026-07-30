# rating_prediction.py
# 电影评分预测（基于特征的回归模型）
# 目标：利用已评分电影（SCORE_FLAG=valid）的特征训练回归模型，预测未评分电影（UnScore）的豆瓣评分
# 特征：年份、类型、地区、语言、投票数、时长
# 模型：随机森林回归 + 线性回归 + GBDT

import os
import sys
import time

# ========== 环境变量配置（必须在 import pyspark 之前） ==========
os.environ["JAVA_HOME"] = r"D:\JDK8"
os.environ["HADOOP_HOME"] = r"E:\hadoop\hadoop-3.3.5"
os.environ["JAVA_TOOL_OPTIONS"] = "-Djava.net.preferIPv4Stack=true"
os.environ["PYSPARK_PYTHON"] = sys.executable
os.environ["PYSPARK_DRIVER_PYTHON"] = sys.executable

java_bin = os.path.join(os.environ["JAVA_HOME"], "bin")
hadoop_bin = os.path.join(os.environ["HADOOP_HOME"], "bin")
os.environ["PATH"] = f"{java_bin};{hadoop_bin};" + os.environ.get("PATH", "")

from pyspark.sql import SparkSession
from pyspark.ml import Pipeline
from pyspark.ml.feature import StringIndexer, OneHotEncoder, VectorAssembler, StandardScaler
from pyspark.ml.regression import RandomForestRegressor, LinearRegression, GBTRegressor
from pyspark.ml.evaluation import RegressionEvaluator
from pyspark.sql.functions import col, coalesce, lit, when, round as spark_round
from pyspark.sql.types import DoubleType

# ========== MySQL 连接配置 ==========
MYSQL_JAR = r"E:\movie\movie_analysiz_01\test02\output\mysql-connector-java-8.0.23.jar"
MYSQL_CONFIG = {
    "url": "jdbc:mysql://localhost:3306/movie_db?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
    "user": "root",
    "password": "M20054921",
    "driver": "com.mysql.cj.jdbc.Driver"
}

# ========== 文件路径 ==========
MOVIE_CSV_DIR = r"E:/movie/movie_analysiz_01/test02/output/data/movie/clean/movies"
OUTPUT_DIR = r"E:/movie_analysiz_01/test02/output/predictions"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# ===========================
# 1. 创建 SparkSession
# ===========================
spark = (SparkSession.builder
         .appName("MovieRatingPrediction")
         .master("local[*]")
         .config("spark.driver.host", "127.0.0.1")
         .config("spark.driver.bindAddress", "127.0.0.1")
         .config("spark.driver.memory", "6g")
         .config("spark.executor.memory", "6g")
         .config("spark.jars", MYSQL_JAR)
         .config("spark.driver.extraClassPath", MYSQL_JAR)
         .config("spark.executor.extraClassPath", MYSQL_JAR)
         .config("spark.sql.adaptive.enabled", "true")
         .config("spark.python.worker.reuse", "false")
         .config("spark.network.timeout", "600s")
         .getOrCreate())
spark.sparkContext.setLogLevel("WARN")

print("=" * 70)
print("电影评分预测系统")
print("=" * 70)

# ===========================
# 2. 读取并准备数据
# ===========================
df_movie_all = spark.read.option("header", "true").csv(MOVIE_CSV_DIR)

total_count = df_movie_all.count()
valid_count = df_movie_all.filter(col("SCORE_FLAG") == "valid").count()
unscore_count = df_movie_all.filter(col("SCORE_FLAG") == "UnScore").count()

print(f"\n电影总数: {total_count}")
print(f"有效评分(训练): {valid_count}")
print(f"未评分(预测目标): {unscore_count}")

# 分离训练集和预测集
df_valid = df_movie_all.filter(col("SCORE_FLAG") == "valid")
df_unscore = df_movie_all.filter(col("SCORE_FLAG") == "UnScore")

# ===========================
# 3. 特征工程
# ===========================
print("\n" + "=" * 70)
print("特征工程")
print("=" * 70)

# 数值字段：空值填充 + 类型转换
df_valid = df_valid.withColumn("YEAR_INT", coalesce(col("YEAR_INT"), lit("0")).cast(DoubleType()))
df_valid = df_valid.withColumn("DOUBAN_VOTES", coalesce(col("DOUBAN_VOTES"), lit("0")).cast(DoubleType()))
df_valid = df_valid.withColumn("MINS", coalesce(col("MINS"), lit("0")).cast(DoubleType()))
df_valid = df_valid.withColumn("DOUBAN_SCORE", col("DOUBAN_SCORE").cast(DoubleType()))

# 类别字段：空值填充
df_valid = df_valid.withColumn("GENRES_CLEAN", coalesce(col("GENRES_CLEAN"), lit("unknown")))
df_valid = df_valid.withColumn("REGIONS_CLEAN", coalesce(col("REGIONS_CLEAN"), lit("unknown")))
df_valid = df_valid.withColumn("LANGUAGES_CLEAN", coalesce(col("LANGUAGES_CLEAN"), lit("unknown")))

# 对 UnScore 数据做同样的填充和类型转换
df_unscore = df_unscore.withColumn("YEAR_INT", coalesce(col("YEAR_INT"), lit("0")).cast(DoubleType()))
df_unscore = df_unscore.withColumn("DOUBAN_VOTES", coalesce(col("DOUBAN_VOTES"), lit("0")).cast(DoubleType()))
df_unscore = df_unscore.withColumn("MINS", coalesce(col("MINS"), lit("0")).cast(DoubleType()))
df_unscore = df_unscore.withColumn("GENRES_CLEAN", coalesce(col("GENRES_CLEAN"), lit("unknown")))
df_unscore = df_unscore.withColumn("REGIONS_CLEAN", coalesce(col("REGIONS_CLEAN"), lit("unknown")))
df_unscore = df_unscore.withColumn("LANGUAGES_CLEAN", coalesce(col("LANGUAGES_CLEAN"), lit("unknown")))

# 构建 Pipeline
# 类别特征编码
genre_indexer = StringIndexer(inputCol="GENRES_CLEAN", outputCol="genres_idx",
                              handleInvalid="keep")
genre_encoder = OneHotEncoder(inputCol="genres_idx", outputCol="genres_vec", dropLast=True)

region_indexer = StringIndexer(inputCol="REGIONS_CLEAN", outputCol="region_idx",
                               handleInvalid="keep")
region_encoder = OneHotEncoder(inputCol="region_idx", outputCol="region_vec", dropLast=True)

lang_indexer = StringIndexer(inputCol="LANGUAGES_CLEAN", outputCol="lang_idx",
                             handleInvalid="keep")
lang_encoder = OneHotEncoder(inputCol="lang_idx", outputCol="lang_vec", dropLast=True)

# 数值特征标准化
num_assembler = VectorAssembler(
    inputCols=["YEAR_INT", "DOUBAN_VOTES", "MINS"],
    outputCol="num_raw_vec", handleInvalid="keep")
num_scaler = StandardScaler(inputCol="num_raw_vec", outputCol="num_scaled_vec",
                            withMean=True, withStd=True)

# 最终特征组装
total_assembler = VectorAssembler(
    inputCols=["genres_vec", "region_vec", "lang_vec", "num_scaled_vec"],
    outputCol="features", handleInvalid="keep")

# 特征工程 Pipeline
feature_stages = [genre_indexer, genre_encoder,
                  region_indexer, region_encoder,
                  lang_indexer, lang_encoder,
                  num_assembler, num_scaler, total_assembler]
feature_pipeline = Pipeline(stages=feature_stages)

# 拟合特征 Pipeline 并转换训练数据
feature_model = feature_pipeline.fit(df_valid)
df_valid_features = feature_model.transform(df_valid)

# 转换 UnScore 数据
df_unscore_features = feature_model.transform(df_unscore)

# 划分训练集/测试集 (70/30)
train_data, test_data = df_valid_features.randomSplit([0.7, 0.3], seed=42)

# 缓存到内存加速多个模型训练
train_data.cache().count()
test_data.cache().count()
print(f"训练集: {train_data.count()} 条")
print(f"测试集: {test_data.count()} 条")

# ===========================
# 4. 训练回归模型
# ===========================
print("\n" + "=" * 70)
print("训练回归模型")
print("=" * 70)

evaluator_rmse = RegressionEvaluator(labelCol="DOUBAN_SCORE", predictionCol="prediction", metricName="rmse")
evaluator_mae = RegressionEvaluator(labelCol="DOUBAN_SCORE", predictionCol="prediction", metricName="mae")
evaluator_r2 = RegressionEvaluator(labelCol="DOUBAN_SCORE", predictionCol="prediction", metricName="r2")

results = []  # 存储各模型评估结果

# ----- 4.1 线性回归（内存友好，优先训练） -----
print("\n--- 线性回归 (LinearRegression) ---")
lr = LinearRegression(featuresCol="features", labelCol="DOUBAN_SCORE",
                      maxIter=100, regParam=0.01, elasticNetParam=0.1)
start = time.time()
lr_model = lr.fit(train_data)
train_time = time.time() - start

lr_predictions = lr_model.transform(test_data)
lr_rmse = evaluator_rmse.evaluate(lr_predictions)
lr_mae = evaluator_mae.evaluate(lr_predictions)
lr_r2 = evaluator_r2.evaluate(lr_predictions)

print(f"  训练耗时: {train_time:.2f}s")
print(f"  RMSE: {lr_rmse:.4f}")
print(f"  MAE:  {lr_mae:.4f}")
print(f"  R²:   {lr_r2:.4f}")
results.append(("LinearRegression", lr_rmse, lr_mae, lr_r2, train_time))

# 释放线性回归中间结果
del lr_predictions

# ----- 4.2 随机森林回归（降低复杂度节约内存） -----
print("\n--- 随机森林回归 (RandomForest) ---")
rf = RandomForestRegressor(featuresCol="features", labelCol="DOUBAN_SCORE",
                           numTrees=20, maxDepth=8, seed=42)
start = time.time()
rf_model = rf.fit(train_data)
train_time = time.time() - start

rf_predictions = rf_model.transform(test_data)
rf_rmse = evaluator_rmse.evaluate(rf_predictions)
rf_mae = evaluator_mae.evaluate(rf_predictions)
rf_r2 = evaluator_r2.evaluate(rf_predictions)

print(f"  训练耗时: {train_time:.2f}s")
print(f"  RMSE: {rf_rmse:.4f}")
print(f"  MAE:  {rf_mae:.4f}")
print(f"  R²:   {rf_r2:.4f}")
results.append(("RandomForest", rf_rmse, rf_mae, rf_r2, train_time))

# 释放随机森林中间结果
del rf_predictions

# ----- 4.3 GBDT 回归（降低迭代次数） -----
print("\n--- GBDT 回归 (GradientBoostedTrees) ---")
gbt = GBTRegressor(featuresCol="features", labelCol="DOUBAN_SCORE",
                   maxIter=20, maxDepth=5, seed=42)
start = time.time()
gbt_model = gbt.fit(train_data)
train_time = time.time() - start

gbt_predictions = gbt_model.transform(test_data)
gbt_rmse = evaluator_rmse.evaluate(gbt_predictions)
gbt_mae = evaluator_mae.evaluate(gbt_predictions)
gbt_r2 = evaluator_r2.evaluate(gbt_predictions)

print(f"  训练耗时: {train_time:.2f}s")
print(f"  RMSE: {gbt_rmse:.4f}")
print(f"  MAE:  {gbt_mae:.4f}")
print(f"  R²:   {gbt_r2:.4f}")
results.append(("GBTRegressor", gbt_rmse, gbt_mae, gbt_r2, train_time))
del gbt_predictions

# ===========================
# 5. 模型对比
# ===========================
print("\n" + "=" * 70)
print("模型性能对比")
print("=" * 70)
print(f"{'模型':<20} {'RMSE':<10} {'MAE':<10} {'R²':<10} {'耗时(s)':<10}")
print("-" * 60)
for name, rmse, mae, r2, t in results:
    print(f"{name:<20} {rmse:<10.4f} {mae:<10.4f} {r2:<10.4f} {t:<10.2f}")

# 选择最佳模型（按 R²）
best_name, best_rmse, best_mae, best_r2, best_time = max(results, key=lambda x: x[3])
print(f"\n最佳模型: {best_name} (R²={best_r2:.4f})")

# ===========================
# 6. 预测 UnScore 电影评分
# ===========================
print("\n" + "=" * 70)
print(f"使用 {best_name} 预测未评分电影评分")
print("=" * 70)

if best_name == "RandomForest":
    best_model = rf_model
elif best_name == "LinearRegression":
    best_model = lr_model
else:
    best_model = gbt_model

# 预测
unscore_preds = best_model.transform(df_unscore_features)

# 整理预测结果：电影ID、名称、预测评分
df_predictions = unscore_preds.select(
    col("MOVIE_ID"),
    col("NAME"),
    col("YEAR_INT"),
    col("GENRES_CLEAN"),
    col("REGIONS_CLEAN"),
    spark_round(col("prediction"), 1).alias("PREDICTED_SCORE")
)

# 保存预测结果到 CSV
pred_csv_path = os.path.join(OUTPUT_DIR, "unscore_predictions.csv")
df_predictions.coalesce(1).write.mode("overwrite").option("header", "true").csv(pred_csv_path)
print(f"预测结果已保存: {pred_csv_path}")

# 显示预测结果概览
df_predictions.show(10, truncate=False)

# 预测评分分布
print("\n预测评分分布:")
df_predictions.withColumn("score_bin",
    when(col("PREDICTED_SCORE") < 3.0, "0-3")
    .when(col("PREDICTED_SCORE") < 5.0, "3-5")
    .when(col("PREDICTED_SCORE") < 7.0, "5-7")
    .otherwise("7-10")
).groupBy("score_bin").count().orderBy("score_bin").show()

# ===========================
# 7. 保存模型
# ===========================
model_path = os.path.join(OUTPUT_DIR, "best_model")
best_model.write().overwrite().save(model_path)
print(f"最佳模型已保存: {model_path}")

# ===========================
# 8. 将评估指标写入 MySQL
# ===========================
print("\n" + "=" * 70)
print("写入评估指标到 MySQL")
print("=" * 70)

from pyspark.sql import Row

from pyspark.sql.types import StructType, StructField, StringType, LongType, DoubleType

# 构造评估指标 DataFrame
metric_schema = StructType([
    StructField("stat_type", StringType(), True),
    StructField("stat_key", StringType(), True),
    StructField("stat_value", StringType(), True),
    StructField("stat_count", LongType(), True),
    StructField("stat_percentage", DoubleType(), True),
    StructField("extra_data", StringType(), True)
])

metric_rows = []
for name, rmse, mae, r2, t in results:
    metric_rows.append(Row(
        stat_type="rating_prediction",
        stat_key=name,
        stat_value=f"RMSE={rmse:.4f}, MAE={mae:.4f}, R²={r2:.4f}",
        stat_count=int(valid_count),
        stat_percentage=float(0),
        extra_data=f'{{"rmse":{rmse},"mae":{mae},"r2":{r2},"train_time_sec":{t}}}'
    ))

# 添加预测统计
pred_count = unscore_preds.count()
pred_avg = df_predictions.agg({"PREDICTED_SCORE": "avg"}).collect()[0][0]
metric_rows.append(Row(
    stat_type="rating_prediction",
    stat_key="prediction_summary",
    stat_value=f"predicted_count={pred_count}, avg_predicted_score={pred_avg:.2f}",
    stat_count=int(pred_count),
    stat_percentage=float(0),
    extra_data=f'{{"best_model":"{best_name}","predicted_count":{pred_count},"avg_predicted_score":{pred_avg:.2f}}}'
))

df_metrics = spark.createDataFrame(metric_rows, schema=metric_schema)

# 写入 MySQL
try:
    df_metrics.write \
        .format("jdbc") \
        .option("url", MYSQL_CONFIG["url"]) \
        .option("dbtable", "movie_stats") \
        .option("user", MYSQL_CONFIG["user"]) \
        .option("password", MYSQL_CONFIG["password"]) \
        .option("driver", MYSQL_CONFIG["driver"]) \
        .mode("append") \
        .save()
    print("✅ 评估指标已写入 MySQL movie_stats 表")
except Exception as e:
    print(f"⚠️ MySQL 写入失败: {e}")
    print("尝试创建 movie_stats 表...")
    # 如果表不存在，先建表
    create_sql = """
    CREATE TABLE IF NOT EXISTS movie_stats (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        stat_type VARCHAR(100),
        stat_key VARCHAR(255),
        stat_value TEXT,
        stat_count BIGINT,
        stat_percentage DECIMAL(10,2),
        extra_data TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """
    try:
        df_metrics.write \
            .format("jdbc") \
            .option("url", MYSQL_CONFIG["url"]) \
            .option("dbtable", "movie_stats") \
            .option("user", MYSQL_CONFIG["user"]) \
            .option("password", MYSQL_CONFIG["password"]) \
            .option("driver", MYSQL_CONFIG["driver"]) \
            .option("createTableOptions", "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4") \
            .mode("append") \
            .save()
        print("✅ 创建 movie_stats 表并写入成功")
    except Exception as e2:
        print(f"❌ MySQL 写入最终失败: {e2}")

# ===========================
# 9. 输出特征重要性（仅树模型）
# ===========================
if best_name != "LinearRegression":
    print("\n" + "=" * 70)
    print(f"{best_name} 特征重要性")
    print("=" * 70)
    feature_cols = ["GENRES(类型)", "REGIONS(地区)", "LANGUAGES(语言)", "YEAR(年份)", "VOTES(投票数)", "MINS(时长)"]
    # 树模型的特征重要性维度对应: genres_vec, region_vec, lang_vec, num_scaled_vec(YEAR, VOTES, MINS)
    # 需要合并: genres_onehot 的多个维度求和
    fi = best_model.featureImportances
    print(f"特征向量维度: {len(fi)}")
    print(f"特征重要性向量: {fi}")

print("\n" + "=" * 70)
print("🎉 评分预测全部完成")
print("=" * 70)

spark.stop()
