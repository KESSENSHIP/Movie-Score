import os
os.environ['JAVA_HOME'] = r'D:\JDK8'
os.environ['HADOOP_HOME'] = r'E:\hadoop\hadoop-3.3.5'

from pyspark.sql import SparkSession
MYSQL_JAR = r'E:\movie\movie_analysiz_01\test02\output\mysql-connector-java-8.0.23.jar'
spark = SparkSession.builder.appName('Check').master('local[*]').config('spark.driver.host','127.0.0.1').config('spark.jars',MYSQL_JAR).config('spark.driver.extraClassPath',MYSQL_JAR).getOrCreate()
spark.sparkContext.setLogLevel('WARN')

url = 'jdbc:mysql://localhost:3306/movie_db?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'

# Check table columns
sql = "(SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_NAME='movie_prediction' AND TABLE_SCHEMA='movie_db') t"
df = spark.read.format('jdbc').option('url',url).option('dbtable',sql).option('user','root').option('password','M20054921').option('driver','com.mysql.cj.jdbc.Driver').load()
print("movie_prediction 表结构:")
df.show(100, truncate=False)

# Check first 3 rows of data
df2 = spark.read.format('jdbc').option('url',url).option('dbtable','movie_prediction').option('user','root').option('password','M20054921').option('driver','com.mysql.cj.jdbc.Driver').load()
print("数据前3行:")
df2.show(3, truncate=False)
print(f"总记录数: {df2.count()}")

spark.stop()
