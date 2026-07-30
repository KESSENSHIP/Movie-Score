import urllib.request, json

data = json.dumps({"username": "admin", "password": "123456"}).encode()
req = urllib.request.Request("http://localhost:8888/api/auth/login", data=data,
    headers={"Content-Type": "application/json"}, method="POST")
resp = urllib.request.urlopen(req)
result = json.loads(resp.read())
print("登录成功, nickname:", result.get("data", {}).get("nickname"))

# 现在查一下 user 表
import os
os.environ["JAVA_HOME"] = r"D:\JDK8"
os.environ["HADOOP_HOME"] = r"E:\hadoop\hadoop-3.3.5"
MYSQL_JAR = r"E:\movie\movie_analysiz_01\test02\output\mysql-connector-java-8.0.23.jar"
from pyspark.sql import SparkSession
spark = SparkSession.builder.appName("Verify").master("local[*]").config("spark.driver.host","127.0.0.1").config("spark.jars",MYSQL_JAR).config("spark.driver.extraClassPath",MYSQL_JAR).getOrCreate()
spark.sparkContext.setLogLevel("WARN")
url = "jdbc:mysql://localhost:3306/movie_db?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
df = spark.read.format("jdbc").option("url",url).option("dbtable","user").option("user","root").option("password","M20054921").option("driver","com.mysql.cj.jdbc.Driver").load()
df.createOrReplaceTempView("u")
# admin 的 md5(username)
import hashlib
admin_md5 = hashlib.md5("admin".encode()).hexdigest()
spark.sql(f"SELECT USER_MD5, USER_NICKNAME FROM u WHERE USER_MD5='{admin_md5}' OR USER_NICKNAME LIKE '%admin%'").show(truncate=False)
spark.stop()
