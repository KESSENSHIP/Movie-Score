from pyspark.sql import SparkSession
from pyspark.sql.functions import col, desc, lit
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import pandas as pd
import mysql.connector
import json

plt.rcParams['font.sans-serif'] = ['SimHei']
plt.rcParams['axes.unicode_minus'] = False

spark = SparkSession.builder \
    .appName("TopRatedVotes") \
    .master("local[*]") \
    .config("spark.driver.extraClassPath", "./mysql-connector-java-8.0.23.jar") \
    .getOrCreate()

movies = spark.read.option("header", True).csv("./data/movie/clean/movies")

# 1. 评分Top20（投票>=100过滤低分小众片）
print("========== 豆瓣评分Top20 ==========")
top_rated = movies.filter(col("DOUBAN_SCORE") > 0) \
    .filter(col("DOUBAN_VOTES") >= 100) \
    .orderBy(desc("DOUBAN_SCORE"), desc("DOUBAN_VOTES")) \
    .select("MOVIE_ID", "NAME", "DOUBAN_SCORE", "DOUBAN_VOTES") \
    .limit(20)
top_rated.show(truncate=False)

# 2. 投票数Top20
print("========== 豆瓣投票Top20 ==========")
top_votes = movies.orderBy(desc(col("DOUBAN_VOTES").cast("int"))) \
    .select("MOVIE_ID", "NAME", "DOUBAN_VOTES", "DOUBAN_SCORE") \
    .limit(20)
top_votes.show(truncate=False)

# ========== 写入MySQL ==========
print("=== 写入MySQL ===")
conn = mysql.connector.connect(
    host="localhost",
    user="root",
    password="M20054921",
    database="movie_db"
)
cursor = conn.cursor()

# 清空旧数据
cursor.execute("DELETE FROM movie_stats WHERE stat_type IN ('top_rated', 'top_votes')")
conn.commit()

insert_sql = """INSERT INTO movie_stats (stat_type, stat_key, stat_value, stat_count, stat_percentage, extra_data) 
                VALUES (%s, %s, %s, %s, %s, %s)"""

# 写入评分Top20
print("写入评分Top20数据...")
rated_rows = top_rated.collect()
for row in rated_rows:
    extra = json.dumps({"movieId": row["MOVIE_ID"], "votes": int(float(row["DOUBAN_VOTES"]))}, ensure_ascii=False)
    cursor.execute(insert_sql, (
        "top_rated",
        row["NAME"],
        str(row["DOUBAN_SCORE"]),
        int(float(row["DOUBAN_VOTES"])),
        None,
        extra
    ))
conn.commit()
print(f"已写入 {len(rated_rows)} 条评分Top20数据")

# 写入投票Top20
print("写入投票Top20数据...")
votes_rows = top_votes.collect()
for row in votes_rows:
    extra = json.dumps({"movieId": row["MOVIE_ID"], "score": float(row["DOUBAN_SCORE"]) if row["DOUBAN_SCORE"] else 0}, ensure_ascii=False)
    cursor.execute(insert_sql, (
        "top_votes",
        row["NAME"],
        str(int(float(row["DOUBAN_VOTES"]))),
        int(float(row["DOUBAN_VOTES"])),
        None,
        extra
    ))
conn.commit()
print(f"已写入 {len(votes_rows)} 条投票Top20数据")

cursor.close()
conn.close()
print("MySQL写入完成")

# ========== 可视化绘图 ==========
# 评分Top20图表
pdf = top_rated.toPandas()
pdf_sorted = pdf.sort_values("DOUBAN_SCORE", ascending=True)
plt.figure(figsize=(10, 8))
plt.barh(pdf_sorted["NAME"], pdf_sorted["DOUBAN_SCORE"].astype(float), color="orange")
plt.xlabel("豆瓣评分")
plt.title("Top20 高评分电影")
plt.tight_layout()
plt.savefig("top_rated.png", dpi=150)
print("评分Top20图已保存 top_rated.png")

# 投票Top20图表
pdf2 = top_votes.toPandas()
pdf2["DOUBAN_VOTES"] = pd.to_numeric(pdf2["DOUBAN_VOTES"], errors="coerce")
pdf2_sorted = pdf2.sort_values("DOUBAN_VOTES", ascending=True)
plt.figure(figsize=(10, 8))
plt.barh(pdf2_sorted["NAME"], pdf2_sorted["DOUBAN_VOTES"], color="green")
plt.xlabel("投票人数")
plt.title("Top20 高投票电影")
plt.tight_layout()
plt.savefig("top_votes.png", dpi=150)
print("投票Top20图已保存 top_votes.png")

spark.stop()
print("程序执行完毕！")
