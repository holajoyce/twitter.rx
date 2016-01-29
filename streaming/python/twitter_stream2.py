import sys

from pyspark import SparkContext
from pyspark.streaming import StreamingContext
from pyspark.streaming.kafka import KafkaUtils
from pyspark.sql import SQLContext, Row
from pyspark import SparkConf
from pyspark.sql.types import *
import json


sc = SparkContext(appName="TwitterApp")
ssc = StreamingContext(sc, 2)
sqlContext = SQLContext(sc)

# brokers, topic = sys.argv[1:]
brokers = "ip-172-31-1-136:9092,ip-172-31-1-137:9092,ip-172-31-1-133:9092,ip-172-31-1-139:9092"
topic = "reddit4"
kafkaStream = KafkaUtils.createDirectStream(ssc, [topic], {"metadata.broker.list": brokers})
lines = kafkaStream.map(lambda x: x[1])

# {"name":"t1_c8770z6","author_flair_css_class":null,"author":"corey3","author_flair_text":null,"ups":1,"id":"c8770z6","edited":false,"retrieved_on":1431145410,"score_hidden":false,"gilded":0,"downs":0,"body":"also try to get ADHD medication.","controversiality":0,"subreddit_id":"t5_2qnwb","distinguished":null,"parent_id":"t1_c85yez1","subreddit":"ADHD","archived":true,"score":1,"link_id":"t3_17itl1"}

# see https://spark.apache.org/docs/1.5.2/sql-programming-guide.html#json-datasets
# r = requests.post("http://localhost:8555/tagbatch", data=df.toJSON())
def process(rdd):
	rowRdd = rdd.map(lambda w: Row(body=json.loads(w)["body"] , author=json.loads(w)["author"] , subreddit=json.loads(w)["subreddit"], retrieved_on=json.loads(w)["retrieved_on"]))
	rowRdd.toDF()
	df_news = sqlContext.createDataFrame(rowRdd)
    df_news.show()
	#tmp = df_news.take(1)
	#print(tmp)


    
"""
	# for row in df_news.collect():
		# print(row)
		# print(row.message)
    # schemaTweet.registerTempTable("Tweet")
	# schemaTweet.show()
	# for row in df_news.collect():
    #    session.execute(st_news, (row.company, row.summary, row.newstime, row.author, row.newsoutlet, row.source, ))
"""
#line = lines.take(1)
#pprint(line)
# lines.foreachRDD(process)
ssc.start()
ssc.awaitTermination()
