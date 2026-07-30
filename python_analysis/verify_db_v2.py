import os, sys
os.environ["JAVA_HOME"] = r"D:\JDK8"
os.environ["HADOOP_HOME"] = r"E:\hadoop\hadoop-3.3.5"
os.environ["JAVA_TOOL_OPTIONS"] = "-Djava.net.preferIPv4Stack=true"

from pyspark.sql import SparkSession

MYSQL_JAR = r"E:\movie\movie_analysiz_01\test02\output\mysql-connector-java-8.0.23.jar"
spark = (SparkSession.builder
         .appName("VerifyDB")
         .master("local[*]")
         .config("spark.driver.host", "127.0.0.1")
         .config("spark.driver.bindAddress", "127.0.0.1")
         .config("spark.driver.memory", "2g")
         .config("spark.jars", MYSQL_JAR)
         .config("spark.driver.extraClassPath", MYSQL_JAR)
         .getOrCreate())
spark.sparkContext.setLogLevel("WARN")

url = "jdbc:mysql://localhost:3306/movie_db?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"

# 验证 movie_prediction 表
df_pred = spark.read.format("jdbc") \
    .option("url", url) \
    .option("dbtable", "movie_prediction") \
    .option("user", "root") \
    .option("password", "M20054921") \
    .option("driver", "com.mysql.cj.jdbc.Driver") \
    .load()

print(f"movie_prediction 表记录数: {df_pred.count()}")
print("评分范围:")
df_pred.createOrReplaceTempView("p")
spark.sql("SELECT MIN(predicted_score), MAX(predicted_score), ROUND(AVG(predicted_score),2) FROM p").show()

# 验证 movie_stats 表
df_stats = spark.read.format("jdbc") \
    .option("url", url) \
    .option("dbtable", "movie_stats") \
    .option("user", "root") \
    .option("password", "M20054921") \
    .option("driver", "com.mysql.cj.jdbc.Driver") \
    .load()
df_stats.createOrReplaceTempView("s")
spark.sql("SELECT stat_key, stat_value, created_at FROM s WHERE stat_type='rating_prediction' ORDER BY id DESC").show(truncate=False)

spark.stop()
