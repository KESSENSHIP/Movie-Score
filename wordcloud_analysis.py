from pyspark.sql import SparkSession
from pyspark.sql.functions import col, split, explode, count, desc, trim, lit
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from wordcloud import WordCloud
import mysql.connector
import json

plt.rcParams['font.sans-serif'] = ['SimHei']
plt.rcParams['axes.unicode_minus'] = False

spark = SparkSession.builder \
    .appName("WordCloudAnalysis") \
    .master("local[*]") \
    .config("spark.driver.extraClassPath", "./mysql-connector-java-8.0.23.jar") \
    .getOrCreate()

movies = spark.read.option("header", True).csv("./data/movie/clean/movies")

# ========== 1. 类型词云 ==========
print("=== 统计电影类型 ===")
genre_df = movies.withColumn("genre", explode(split(col("GENRES_CLEAN"), "/"))) \
    .filter(col("genre") != "") \
    .withColumn("genre", trim(col("genre")))
genre_counts = genre_df.groupBy("genre").agg(count("MOVIE_ID").alias("count")).orderBy(desc("count"))
genre_counts.show(30)

genre_dict = {row["genre"]: int(row["count"]) for row in genre_counts.collect() if row["genre"] != "Unknown"}
print(f"类型数量（已过滤Unknown）: {len(genre_dict)}")

# 生成类型词云
try:
    wc = WordCloud(font_path="simhei.ttf", background_color="white", width=800, height=400, max_words=100)
    wc.generate_from_frequencies(genre_dict)
    wc.to_file("genre_wordcloud.png")
    print("类型词云已保存为 genre_wordcloud.png")
except Exception as e:
    print(f"类型词云生成失败（可能缺少字体）: {e}")
    # 降级：用matplotlib绘制横向条形图
    top20 = genre_counts.limit(20).toPandas()
    plt.figure(figsize=(12, 8))
    plt.barh(top20["genre"][::-1], top20["count"][::-1], color="skyblue")
    plt.xlabel("电影数量")
    plt.title("Top20 电影类型")
    plt.tight_layout()
    plt.savefig("genre_wordcloud.png", dpi=150)
    print("类型条形图已保存为 genre_wordcloud.png")

# ========== 2. 标签词云 ==========
print("=== 统计电影标签 ===")
if "TAGS" in movies.columns:
    tags_df = movies.withColumn("tag", explode(split(col("TAGS"), "/"))) \
        .filter(col("tag") != "") \
        .withColumn("tag", trim(col("tag")))
    tag_counts = tags_df.groupBy("tag").agg(count("MOVIE_ID").alias("count")).orderBy(desc("count"))
    tag_counts.show(30)
    
    tag_dict = {row["tag"]: int(row["count"]) for row in tag_counts.collect() if row["tag"] != "Unknown"}
    print(f"标签数量（已过滤Unknown）: {len(tag_dict)}")
    
    # 生成标签词云
    try:
        wc2 = WordCloud(font_path="simhei.ttf", background_color="white", width=800, height=400, max_words=100)
        wc2.generate_from_frequencies(tag_dict)
        wc2.to_file("tag_wordcloud.png")
        print("标签词云已保存为 tag_wordcloud.png")
    except Exception as e:
        print(f"标签词云生成失败（可能缺少字体）: {e}")
        top20_tags = tag_counts.limit(20).toPandas()
        plt.figure(figsize=(12, 8))
        plt.barh(top20_tags["tag"][::-1], top20_tags["count"][::-1], color="salmon")
        plt.xlabel("电影数量")
        plt.title("Top20 电影标签")
        plt.tight_layout()
        plt.savefig("tag_wordcloud.png", dpi=150)
        print("标签条形图已保存为 tag_wordcloud.png")

# ========== 3. 保存到MySQL ==========
print("=== 写入MySQL ===")
conn = mysql.connector.connect(
    host="localhost",
    user="root",
    password="M20054921",
    database="movie_db"
)
cursor = conn.cursor()

insert_sql = """INSERT INTO movie_stats (stat_type, stat_key, stat_value, stat_count, stat_percentage, extra_data) 
                VALUES (%s, %s, %s, %s, %s, %s)"""

# 清空旧数据
cursor.execute("DELETE FROM movie_stats WHERE stat_type IN ('genre_cloud', 'tag_cloud')")
conn.commit()

# 写入类型统计（取Top100，过滤Unknown）
print("写入类型统计数据...")
genre_top = genre_counts.filter(col("genre") != "Unknown").limit(100).collect()
for row in genre_top:
    cursor.execute(insert_sql, (
        "genre_cloud",
        row["genre"],
        "",
        int(row["count"]),
        None,
        json.dumps({"count": int(row["count"])}, ensure_ascii=False)
    ))
conn.commit()
print(f"已写入 {len(genre_top)} 条类型数据")

# 写入标签统计（取Top200，过滤Unknown）
print("写入标签统计数据...")
if "TAGS" in movies.columns:
    tag_top = tag_counts.filter(col("tag") != "Unknown").limit(200).collect()
    for row in tag_top:
        cursor.execute(insert_sql, (
            "tag_cloud",
            row["tag"],
            "",
            int(row["count"]),
            None,
            json.dumps({"count": int(row["count"])}, ensure_ascii=False)
        ))
    conn.commit()
    print(f"已写入 {len(tag_top)} 条标签数据")

cursor.close()
conn.close()
print("MySQL写入完成")

spark.stop()
