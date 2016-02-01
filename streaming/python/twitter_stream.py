# http://zdatainc.com/2014/08/real-time-streaming-apache-spark-streaming/
# HI!!
"""
 Counts words in UTF8 encoded, '\n' delimited text received from the network every second.
 Usage: kafka_wordcount.py <zk> <topic>

 To run this on your local machine, you need to setup Kafka and create a producer first, see
 http://kafka.apache.org/documentation.html#quickstart

 and then run the example
    `$ bin/spark-submit --jars \
      external/kafka-assembly/target/scala-*/spark-streaming-kafka-assembly-*.jar \
      examples/src/main/python/streaming/kafka_wordcount.py \
      localhost:2181 test`
"""

from __future__ import print_function

import sys

from pyspark import SparkContext
from pyspark.streaming import StreamingContext
from pyspark.streaming.kafka import KafkaUtils
from pyspark.sql import SQLContext, Row
from pyspark.sql.functions import explode
# from pyspark.sql.DataFrame import alias
import json
import requests
from time import gmtime, strftime
import unicodedata,re
import pyspark_cassandra


# line="""{"subreddit_id":"t5_2yljs","score":0,"edited":false,"name":"t1_cekta7f","author_flair_css_class":"default","author":"Kimera25","parent_id":"t1_ceks74z","link_id":"t3_1uqrin","retrieved_on":1431859483,"score_hidden":false,"author_flair_text":"360 100%","subreddit":"chiliadmystery","downs":0,"removal_reason":null,"controversiality":0,"id":"cekta7f","ups":0,"gilded":0,"distinguished":null,"body":"I just tried it and nothing, it's a half moon on a tuesday, no rain, i'll try it with a thunderstorm next time. this got me thinking that the mural could have been painted by the Altruists and each of the X's represents a sacrifice and you have to do the fifth one, I guess on chop. random theory built off all this.","archived":true}"""
# https://github.com/apache/spark/blob/master/examples/src/main/python/streaming/sql_network_wordcount.py
# https://github.com/willzfarmer/TwitterPanic/blob/master/python/analysis.py
# https://raw.githubusercontent.com/rustyrazorblade/killranalytics/intro_streaming_python2/killranalytics/spark/raw_event_stream_processing.py
# https://rideondata.wordpress.com/2015/06/29/analyzing-wikipedia-text-with-pyspark/
# https://github.com/andyikchu/insightproject/blob/master/realtime_processing/twitter_stream.py
# https://github.com/willzfarmer/TwitterPanic/tree/master/python
# https://docs.cloud.databricks.com/docs/latest/databricks_guide/08%20Spark%20Streaming/06%20FileStream%20Word%20Count%20-%20Python.html

# difference between transform and map
# https://mail-archives.apache.org/mod_mbox/spark-user/201402.mbox/%3CCAMwrk0nmMy7-9Q0nkoUBhm+X42O4=-nMDyMFGG2VzrdtJa_7_g@mail.gmail.com%3E

# joining streams
# https://spark.apache.org/docs/latest/streaming-programming-guide.html

# multiple d-streams (tDas response)
# http://apache-spark-user-list.1001560.n3.nabble.com/using-multiple-dstreams-together-spark-streaming-td9947.html

# tabular data
# https://www.mapr.com/blog/using-apache-spark-dataframes-processing-tabular-data

# joining dataframes (see section, a more concrete example)
# https://spark.apache.org/docs/1.5.2/api/python/pyspark.sql.html

# how not to duplicate columns
# https://forums.databricks.com/questions/876/is-there-a-better-method-to-join-two-dataframes-an.html

# cassandra
# https://github.com/datastax/spark-cassandra-connector/blob/master/doc/15_python.md
# https://github.com/TargetHolding/pyspark-cassandra # even easier!

def getSqlContextInstance(sparkContext):
  if ('sqlContextSingletonInstance' not in globals()):
      globals()['sqlContextSingletonInstance'] = SQLContext(sparkContext)
  return globals()['sqlContextSingletonInstance']

# conf = SparkConf() \
#   .setAppName("PySpark Cassandra Text Bids Join") \
#   .set("spark.cassandra.connection.host", "cas-1")

# sc = CassandraSparkContext(conf=conf)
sc = SparkContext(appName="stream_tagger")
ssc = StreamingContext(sc, 1)

zkQuorum, topic = sys.argv[1:]
#brokers, topic = sys.argv[1:]
#stream = KafkaUtils.createDirectStream(ssc, [topic], {"metadata.broker.list": brokers})
stream = KafkaUtils.createStream(ssc, zkQuorum, "spark-streaming-consumer", {topic: 1})
stream2 = KafkaUtils.createStream(ssc, zkQuorum, "spark-streaming-consumer2", {"pharma_bids_prices2": 1})

datasourcetype = "TT" if topic=="TT_raw" else "RD"
tagger_url = "http://localhost:8555/tagbatch/"+datasourcetype

lines_count =0;
LINES_COUNT_MAX=100;

control_chars = ''.join(map(unichr, range(0,32) + range(127,160)))
control_char_re = re.compile('[%s]' % re.escape(control_chars))



def process(rdd):
  print(">>>> END")
  # send to td-agent, to send to elasticsearch
  print(rdd.take(10))
  print(">>>> END")

def tfunc(t,rdd,rddb):
  # texts
  try:
    #----- texts

    rowRdd = rdd.map(lambda w: Row(id=w['id'],author=w['user_screen_name'], body=w['body'], created_utc=w['created_utc'], pharmatags=w['pharmatags'], conditiontags=w['conditiontags'], symptomtags=w['symptomtags']))
    texts = getSqlContextInstance(rdd.context).createDataFrame(rowRdd) 
    texts.registerTempTable("texts")
    texts = texts.select(texts.id,texts.created_utc,texts.author,texts.body, explode(texts.pharmatags).alias('pharmatag'), texts.conditiontags, texts.symptomtags)

    #----- bids
    rowRdd2= rddb.map(lambda w: Row(price=w['price'], pharmatag=w['pharmatags']))
    bids = getSqlContextInstance(rddb.context).createDataFrame(rowRdd2) 
    bids.registerTempTable("bids")
    bids = bids.select(bids.price,bids.pharmatag)
    
    #---- texts ids joined with pharma bids
    idbids = texts.join(bids,texts.pharmatag==bids.pharmatag,'inner').select(texts.id,texts.author, texts.created_utc, texts.body, texts.conditiontags, texts.symptomtags, bids.pharmatag,bids.price)
    idbids.registerTempTable("idbids")

    #-----texts id & bids, find min
    idsbidsmin = getSqlContextInstance(rddb.context).sql("SELECT id, author, created_utc, body, pharmatag, conditiontags, symptomtags, max(price) as price FROM idbids GROUP BY id,author, created_utc, body, conditiontags, symptomtags, pharmatag ")
    idsbidsmin.registerTempTable("idsbidsmin") # dataframe

    return idsbidsmin.rdd

  except 'Exception':
    pass
#------------
# 2 different streams 1 tweets, the other bids from pharmaceutical companies
lines_texts = stream.map(lambda x:    requests.post(tagger_url,data=json.dumps(json.loads( control_char_re.sub('',x[1]))) ).json() )  
lines_bids = stream2.map(lambda x: json.loads(x[1])   ) 

# join the streams together
lines_texts_with_bids = lines_texts.transformWith(tfunc, lines_bids)

# get back a new type of rdd & process
lines_texts_with_bids.foreachRDD(process) 

ssc.start()
ssc.awaitTermination()

