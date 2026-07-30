from pyspark.sql import SparkSession
from pyspark.sql.functions import col, split, explode, count, desc, lit, to_json, struct, trim
from pyspark.sql.types import *
import matplotlib.pyplot as plt
import pandas as pd

# 解决matplotlib中文乱码
plt.rcParams['font.sans-serif'] = ['SimHei']
plt.rcParams['axes.unicode_minus'] = False

# 构建Spark会话
spark = SparkSession.builder \
    .appName("RegionTop50") \
    .master("local[*]") \
    .config("spark.driver.extraClassPath", "./mysql-connector-java-8.0.23.jar") \
    .getOrCreate()

# 读取清洗后的电影csv数据（从test02的数据路径）
movies = spark.read.option("header", True).csv("./data/movie/clean/movies")

# 拆分地区、过滤空值、清洗地区前后空格（解决"中国大陆 " 和 "中国大陆" 分开统计问题）
region_df = movies.withColumn("region", explode(split(col("REGIONS"), "/"))) \
    .filter(col("region") != "") \
    .withColumn("region", trim(col("region")))

# 按地区分组统计电影数量，取Top50
region_counts = region_df.groupBy("region") \
    .agg(count("MOVIE_ID").alias("count")) \
    .orderBy(desc("count")) \
    .limit(50)

print("各国家/地区电影发行量Top50：")
region_counts.show(50)

# 计算总电影数用于百分比计算
total_movies = region_df.count()

# 构造入库统计表结构
stats_df = region_counts.select(
    lit("region_top50").alias("stat_type"),
    col("region").alias("stat_key"),
    lit(None).cast("string").alias("stat_value"),
    col("count").alias("stat_count"),
    lit(None).cast("decimal(10,2)").alias("stat_percentage"),
    to_json(struct("count")).alias("extra_data")
)

# ========== 核心修复：URL添加allowPublicKeyRetrieval=true ==========
stats_df.write \
    .format("jdbc") \
    .option("url", "jdbc:mysql://localhost:3306/movie_db?useSSL=false&allowPublicKeyRetrieval=true") \
    .option("dbtable", "movie_stats") \
    .option("user", "root") \
    .option("password", "M20054921") \
    .option("driver", "com.mysql.cj.jdbc.Driver") \
    .mode("append") \
    .save()

print("数据已保存到MySQL movie_stats表")

# Pandas转为本地数据，绘制Top50柱状图
pdf = region_counts.toPandas()

# 绘制Top20柱状图（更清晰）
plt.figure(figsize=(14, 8))
plt.bar(pdf["region"][:20], pdf["count"][:20], color="skyblue")
plt.xticks(rotation=45, ha="right")
plt.xlabel("地区")
plt.ylabel("电影数量")
plt.title("Top20 地区电影发行量")
plt.tight_layout()
plt.savefig("region_top20.png", dpi=150)
print("地区Top20柱状图已保存至当前目录")

# 绘制完整Top50柱状图
plt.figure(figsize=(20, 10))
plt.bar(pdf["region"], pdf["count"], color="steelblue")
plt.xticks(rotation=60, ha="right", fontsize=8)
plt.xlabel("地区")
plt.ylabel("电影数量")
plt.title("Top50 地区电影发行量")
plt.tight_layout()
plt.savefig("region_top50.png", dpi=150)
print("地区Top50柱状图已保存至当前目录")

# 关闭Spark会话释放资源
spark.stop()
