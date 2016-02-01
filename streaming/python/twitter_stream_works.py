# http://zdatainc.com/2014/08/real-time-streaming-apache-spark-streaming/

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
import json
import requests

# https://github.com/apache/spark/blob/master/examples/src/main/python/streaming/sql_network_wordcount.py
# https://github.com/willzfarmer/TwitterPanic/blob/master/python/analysis.py
# https://raw.githubusercontent.com/rustyrazorblade/killranalytics/intro_streaming_python2/killranalytics/spark/raw_event_stream_processing.py
# https://rideondata.wordpress.com/2015/06/29/analyzing-wikipedia-text-with-pyspark/
# https://github.com/andyikchu/insightproject/blob/master/realtime_processing/twitter_stream.py
# http://will-farmer.com/twitter-civil-unrest-analysis-with-apache-spark.html

sc = SparkContext(appName="stream_tagger")
ssc = StreamingContext(sc, 1)
zkQuorum, topic = sys.argv[1:]
datasourcetype = "TT" if topic=="TT_raw" else "RD"
tagger_url = "http://localhost:8555/tagbatch/"+datasourcetype

def getSqlContextInstance(sparkContext):
  if ('sqlContextSingletonInstance' not in globals()):
      globals()['sqlContextSingletonInstance'] = SQLContext(sparkContext)
  return globals()['sqlContextSingletonInstance']


stream = KafkaUtils.createStream(ssc, zkQuorum, "spark-streaming-consumer", {topic: 1})




def parse_json(v):
  return (None, json.loads(v))

def process(time, rdd):
  print("========= %s =========" % str(time))
  try:
    rowRdd = rdd.map(lambda w: Row(author=w['user_screen_name'], body=w['body'], created_utc=w['created_utc']))
    df = getSqlContextInstance(rdd.context).createDataFrame(rowRdd) 
    df.registerTempTable("df")
    df_jsons = df.toJSON()
    first = df_jsons.first()
    print(first)
    #df_json.show()
  except:
    pass

#stream = stream.transform(tfunc)

#lines_texts = stream.map(lambda x:    json.loads(requests.post(tagger_url,data=json.dumps(json.loads(x[1])) ).text)  )
parsed = stream.map(lambda (k,v): requests.post(tagger_url,data=  json.loads(json.dumps(json.loads(v)) ).text))
#parsed = stream.map(lambda (k,v): json.loads(v))
parsed.foreachRDD(process)
ssc.start()
ssc.awaitTermination()

