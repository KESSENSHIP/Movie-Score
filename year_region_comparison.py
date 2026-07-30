from pyspark.sql import SparkSession
from pyspark.sql.functions import col, split, explode, count, desc, trim, sum as spark_sum
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import json
import mysql.connector

plt.rcParams['font.sans-serif'] = ['SimHei']
plt.rcParams['axes.unicode_minus'] = False

spark = SparkSession.builder \
    .appName("YearRegionComparison") \
    .master("local[*]") \
    .config("spark.driver.extraClassPath", "./mysql-connector-java-8.0.23.jar") \
    .getOrCreate()

movies = spark.read.option("header", True).csv("./data/movie/clean/movies")

region_year_df = movies.withColumn("region", explode(split(col("REGIONS"), "/"))) \
    .filter(col("region") != "") \
    .withColumn("region", trim(col("region"))) \
    .filter(col("YEAR_INT").cast("int") > 0)

year_region_counts = region_year_df.groupBy("YEAR_INT", "region") \
    .agg(count("MOVIE_ID").alias("count"))

top_regions = region_year_df.groupBy("region").count().orderBy(desc("count")).limit(10)
top_region_list = [row.region for row in top_regions.collect()]
print(f"Top10地区: {top_region_list}")

filtered = year_region_counts.filter(col("region").isin(top_region_list))

pivot_df = filtered.groupBy("YEAR_INT").pivot("region").sum("count").fillna(0).orderBy("YEAR_INT")
pivot_df.show(50)

# 使用collect代替toPandas，避免Windows下Python worker超时
pivot_rows = pivot_df.collect()
years = [row["YEAR_INT"] for row in pivot_rows]
region_data_map = {}
for region in top_region_list:
    region_data_map[region] = [row[region] for row in pivot_rows]

# 绘制折线图
fig, ax = plt.subplots(figsize=(14, 8))
colors = ['#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#909399', '#9b59b6', '#1abc9c', '#e74c3c', '#3498db', '#f39c12']
for i, region in enumerate(top_region_list):
    ax.plot(years, region_data_map[region], marker='o', linewidth=2, 
            label=region, color=colors[i % len(colors)])
ax.set_xlabel("年份", fontsize=14)
ax.set_ylabel("发行电影数", fontsize=14)
ax.set_title("Top10地区每年电影发行量对比", fontsize=16)
ax.legend(title="地区", fontsize=9, loc='upper left', bbox_to_anchor=(1, 1))
ax.grid(alpha=0.3)
plt.tight_layout()
plt.savefig("year_region_comparison.png", dpi=150)
print("折线图已保存为 year_region_comparison.png")

# 构建入库数据并直接用mysql-connector写入
print("正在构建入库数据...")
conn = mysql.connector.connect(
    host="localhost",
    user="root",
    password="M20054921",
    database="movie_db"
)
cursor = conn.cursor()

# 清除旧数据
cursor.execute("DELETE FROM movie_stats WHERE stat_type = 'year_region_comparison'")
conn.commit()

insert_sql = """INSERT INTO movie_stats (stat_type, stat_key, stat_value, stat_count, stat_percentage, extra_data) 
                VALUES (%s, %s, %s, %s, %s, %s)"""

for region in top_region_list:
    region_data = year_region_counts.filter(col("region") == region)
    total_count = region_data.agg(spark_sum("count").alias("total")).collect()[0]["total"] or 0
    
    year_counts = {}
    for row in region_data.collect():
        year_counts[str(row["YEAR_INT"])] = int(row["count"])
    
    extra_json_str = json.dumps(year_counts, ensure_ascii=False) if year_counts else "{}"
    
    cursor.execute(insert_sql, (
        "year_region_comparison",
        region,
        "",
        int(total_count),
        None,
        extra_json_str
    ))

conn.commit()
print(f"数据已保存到MySQL movie_stats表 (stat_type=year_region_comparison), 共 {cursor.rowcount} 条记录")

# 查询验证
cursor.execute("SELECT stat_key, stat_count FROM movie_stats WHERE stat_type = 'year_region_comparison' ORDER BY stat_count DESC")
for row in cursor.fetchall():
    print(f"  {row[0]}: {row[1]}")

cursor.close()
conn.close()

spark.stop()
