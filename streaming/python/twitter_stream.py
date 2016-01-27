import sys

from pyspark import SparkContext
from pyspark.streaming import StreamingContext
from pyspark.streaming.kafka import KafkaUtils
from pyspark.sql import SQLContext, Row

from pyspark import SparkConf
from pyspark.sql.types import *
#from cassandra.cluster import Cluster
#from cassandra import ConsistencyLevel
#from cqlengine import connection
#from cqlengine import columns
#from cqlengine.models import Model
#from cqlengine.management import sync_table

# ingest from kafka

sc = SparkContext(appName="TwitterApp")
ssc = StreamingContext(sc, 2)
sqlContext = SQLContext(sc)
import json

brokers, topic = sys.argv[1:]
kafkaStream = KafkaUtils.createDirectStream(ssc, [topic], {"metadata.broker.list": brokers})
lines = kafkaStream.map(lambda x: x[1])

def process(rdd):
    # see https://spark.apache.org/docs/1.5.2/sql-programming-guide.html#json-datasets
    # convert each line to a Row
	#df=sqlContext.createDataFrame(rdd)
	#df.show()
	rowRdd = rdd.map(lambda w: Row(message=json.loads(w)["message"], user_screen_name=json.loads(w)["user_screen_name"],  ))

	r = requests.get('https://api.github.com/user', auth=('user', 'pass'))


	df_news = sqlContext.createDataFrame(rowRdd)
	tmp = df_news.take(1)
	print(tmp)
	#for row in df_news.collect():
		#print(row)
		#print(row.message)
    #schemaTweet.registerTempTable("Tweet")
	#schemaTweet.show()
    
	# for row in df_news.collect():
    #    session.execute(st_news, (row.company, row.summary, row.newstime, row.author, row.newsoutlet, row.source, ))

lines.foreachRDD(process)

# counts = lines.flatMap(lambda line: line.split(" ")) \
#     .map(lambda word: (word, 1)) \
#     .reduceByKey(lambda a, b: a+b)
# counts.pprint()



ssc.start()
ssc.awaitTermination()
