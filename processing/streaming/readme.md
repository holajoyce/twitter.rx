#--------------------------
#------- python---------
#--------------------------

sudo pip install cassandra-driver
sudo pip install cqlengine
sudo pip install pykafka
sudo apt-get install librdkafka-dev

git@github.com:tdas/spark-streaming-external-projects.git

spark-submit --jars spark-streaming-kafka-assembly_2.10-1.3.1.jar Script.py 
instead of 
spark-submit Script.py --jars spark-streaming-kafka-assembly_2.10-1.3.1.jar.

$SPARK_HOME/bin/spark-submit --jars spark-streaming-kafka-assembly_2.10-1.3.1.jar  twitter_stream.py


pyspark --jars spark-streaming-kafka-assembly_2.10-1.3.1.jar --master local[4]

import simplejson as json
from pyspark import SparkContext
rrd = sc.textFile("/user/twitter.json")
data = rrd.map(lambda line: json.loads(line))
despide = data.filter(lambda tweet: "despide" in tweet.get("text",""))
	print despide.count()

#--------
# 1
from pyspark.sql import SQLContext, Row
sqlContext = SQLContext(sc)

lines = sc.textFile("/home/ubuntu/spark/examples/src/main/resources/people.txt")
parts = lines.map(lambda l: l.split(","))
people = parts.map(lambda p: Row(name=p[0], age=int(p[1])))
schemaPeople = sqlContext.createDataFrame(people)
schemaPeople.registerTempTable("people")
schemaPeople.show()


# 2
from pyspark.sql import SQLContext
sqlContext = SQLContext(sc)
peeps = sqlContext.read.json("/home/ubuntu/spark/examples/src/main/resources/people.json")
peeps.show()
people.registerTempTable("people")
people.show()

#----------
lines = sc.textFile("/home/ubuntu/spark/examples/src/main/resources/people.txt")
parts = lines.map(lambda l: l.split(","))
people = parts.map(lambda p: Row(name=p[0], age=int(p[1])))

#--------

spark-submit --packages org.apache.spark:spark-streaming-kafka_2.10:1.5.1 kafka_wordcount.py ec2-52-33-248-41.us-west-2.compute.amazonaws.com:2181 TT_raw


spark-submit --packages org.apache.spark:spark-streaming-kafka_2.10:1.5.1 direct_kafka_wordcount.py "ec2-52-33-248-41.us-west-2.compute.amazonaws.com:9092,ec2-52-88-121-199.us-west-2.compute.amazonaws.com:9092" "TT_raw"


spark-submit --packages org.apache.spark:spark-streaming-kafka_2.10:1.5.1 twitter_stream.py "ec2-52-33-248-41.us-west-2.compute.amazonaws.com:9092,ec2-52-88-121-199.us-west-2.compute.amazonaws.com:9092" "TT_raw"



#--------------------------
#----------- scala -----
#--------------------------

run-example org.apache.spark.examples.streaming.KafkaWordCount ec2-52-35-9-210.us-west-2.compute.amazonaws.com:2181,ec2-52-88-121-199.us-west-2.compute.amazonaws.com:2181,ec2-52-88-208-18.us-west-2.compute.amazonaws.com:2181 my-consumer-group TT_raw 2

# good refernces for various examples
https://github.com/pbashyal-nmdp/spark-training

spark-submit \
     --class "com.databricks.apps.twitter_classifier.Collect" \
     --master local[4] \
     target/scala-2.10/spark-twitter-lang-classifier-assembly-1.0.jar \
     tweets \
     10000 \
     10 \
     1 \
     --consumerKey "EbLea0pHXSrc9E7l3yZZ1SGKX" \
     --consumerSecret "grJcQNXgDITHIaWyYq3S21UPWFUN7ZKAWDDYKcmToL6qjJjPls" \
     --accessToken "3255564181-v5jpx9xfBR7K8ottr1DBVB2dZV4utPAoayZRKDU" \
     --accessTokenSecret "M1LrHbo5qsj8Wdyy9qiDbpsZFVMjVgoCShoDdQvPd6ySd"

# --- train model
spark-submit \
     --class "com.databricks.apps.twitter_classifier.ExamineAndTrain" \
     --master local \
     target/scala-2.10/spark-twitter-lang-classifier-assembly-1.0.jar \
     "tweets/tweets*/part-*" \
     "/home/ubuntu/tweets/model" \
     3 \
     4

#---- predict
spark-submit \
     --class "com.databricks.apps.twitter_classifier.Predict" \
     --master local \
     target/scala-2.10/spark-twitter-lang-classifier-assembly-1.0.jar \
     tweets/model \
     1 \
     --consumerKey "EbLea0pHXSrc9E7l3yZZ1SGKX" \
     --consumerSecret "grJcQNXgDITHIaWyYq3S21UPWFUN7ZKAWDDYKcmToL6qjJjPls" \
     --accessToken "3255564181-v5jpx9xfBR7K8ottr1DBVB2dZV4utPAoayZRKDU" \
     --accessTokenSecret "M1LrHbo5qsj8Wdyy9qiDbpsZFVMjVgoCShoDdQvPd6ySd"
#----------------------------------
- put the jar onto the path
spark-shell  --jars spark-streaming-kafka-assembly_2.10-1.3.1.jar

#---- how to run wordcount spark scala----
http://www.cnblogs.com/hseagle/p/3887507.html


#---------------------------
#---- spark examples---------
# data frames
val df = sqlContext.read.json("/home/ubuntu/spark/examples/src/main/resources/people.json")

#---------------
# https://spark.apache.org/docs/1.5.2/sql-programming-guide.html#inferring-the-schema-using-reflection
import sqlContext.implicits._

case class Person(name: String, age: Int)

val people = sc.textFile("/home/ubuntu/spark/examples/src/main/resources/people.txt").map(_.split(",")).map(p => Person(p(0), p(1).trim.toInt)).toDF()


val teenagers = people.where('age >= 13).where('age <= 19).select('name)
teenagers.explain
teenagers.collect()

teenagers.map(t => "Name: " + t(0)).collect().foreach(println) // there's a collect in here

#------------------- ****
//see https://spark.apache.org/docs/1.5.2/sql-programming-guide.html#udf-registration-moved-to-sqlcontextudf-java--scala
import sqlContext.implicits._

val path = "/home/ubuntu/spark/examples/src/main/resources/people.json"
val peeps = sqlContext.read.json(path) // dataframe
peeps.registerTempTable("people")
val teenies = sqlContext.sql("SELECT name FROM people WHERE age >= 13 AND age <= 19")

#-------- Data sources

val df = sqlContext.read.load("/home/ubuntu/spark/examples/src/main/resources/users.parquet")
df.select("name", "favorite_color").write.save("/home/ubuntu/spark/examples/src/main/resources/namesAndFavColors.parquet")

val df2  = sqlContext.read.load("/home/ubuntu/spark/examples/src/main/resources/namesAndFavColors.parquet")


#---- merging schema

val df1 = sc.makeRDD(1 to 5).map(i => (i, i * 2)).toDF("single", "double")
df1.write.parquet("data/test_table/key=1")

val df2 = sc.makeRDD(6 to 10).map(i => (i, i * 3)).toDF("single", "triple")
df2.write.parquet("data/test_table/key=2")

val df3 = sqlContext.read.option("mergeSchema", "true").parquet("data/test_table")
df3.printSchema()

#-----------------
#------java-------
#----------------


spark-submit --packages "org.apache.spark:spark-streaming-kafka_2.10:1.5.1"  --class com.insightde.TwitterStreamProcess --master ec2-52-33-29-117.us-west-2.compute.amazonaws.com:7077  target/SMA-0.0.7-SNAPSHOT.jar ec2-52-33-248-41.us-west-2.compute.amazonaws.com:2181 my-consumer-group TT_raw 3
