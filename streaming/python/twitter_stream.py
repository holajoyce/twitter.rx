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
from time import gmtime, strftime
import unicodedata

# line="""{"subreddit_id":"t5_2yljs","score":0,"edited":false,"name":"t1_cekta7f","author_flair_css_class":"default","author":"Kimera25","parent_id":"t1_ceks74z","link_id":"t3_1uqrin","retrieved_on":1431859483,"score_hidden":false,"author_flair_text":"360 100%","subreddit":"chiliadmystery","downs":0,"removal_reason":null,"controversiality":0,"id":"cekta7f","ups":0,"gilded":0,"distinguished":null,"body":"I just tried it and nothing, it's a half moon on a tuesday, no rain, i'll try it with a thunderstorm next time. this got me thinking that the mural could have been painted by the Altruists and each of the X's represents a sacrifice and you have to do the fifth one, I guess on chop. random theory built off all this.","archived":true}"""
# https://github.com/apache/spark/blob/master/examples/src/main/python/streaming/sql_network_wordcount.py
# https://github.com/willzfarmer/TwitterPanic/blob/master/python/analysis.py
# https://raw.githubusercontent.com/rustyrazorblade/killranalytics/intro_streaming_python2/killranalytics/spark/raw_event_stream_processing.py
# https://rideondata.wordpress.com/2015/06/29/analyzing-wikipedia-text-with-pyspark/
# https://github.com/andyikchu/insightproject/blob/master/realtime_processing/twitter_stream.py
# https://github.com/willzfarmer/TwitterPanic/tree/master/python
# https://docs.cloud.databricks.com/docs/latest/databricks_guide/08%20Spark%20Streaming/06%20FileStream%20Word%20Count%20-%20Python.html

sc = SparkContext(appName="stream_tagger")
ssc = StreamingContext(sc, 1)
tagger_url = "http://localhost:8555/tagbatch"


def getSqlContextInstance(sparkContext):
  if ('sqlContextSingletonInstance' not in globals()):
      globals()['sqlContextSingletonInstance'] = SQLContext(sparkContext)
  return globals()['sqlContextSingletonInstance']

zkQuorum, topic = sys.argv[1:]
stream = KafkaUtils.createStream(ssc, zkQuorum, "spark-streaming-consumer", {topic: 1})

lines_count =0;
LINES_COUNT_MAX=100;

def tfunc_transform_data_stream(t, rdd):
  """
  Transforming function. Converts our blank RDD to something usable
  :param t: datetime
  :param rdd: rdd
      Current rdd we're mapping to
  """
  return rdd.flatMap(lambda x: transform_data_stream)



def transform_data_stream(json_line):
  print("............. transform data stream method starting "+strftime("%Y-%m-%d %H:%M:%S", gmtime())+" .............")
  # lineTuple.pprint()
  line_enriched_json = requests.post(tagger_url,data=json.dumps(json_line)).json()
  jstr = json.dumps(line_enriched_json)
  print(jstr)
  #line_enriched = line_enriched.encode('ascii','ignore')
  #lineTuple = (lineTuple[0],jstr)
  print("............. end data stream method starting "+strftime("%Y-%m-%d %H:%M:%S", gmtime())+" ............." )
  #yield line_enriched.text.encode("utf-8")
  yield jstr
  # data      = [('language', 'en'), ('locations', '-130,20,-60,50')]
  # # query_url = config.url + '?' + '&'.join([str(t[0]) + '=' + str(t[1]) for t in data])
  # post = json.loads(line.decode('utf-8'))
  # print(line)


def process(time, rdd):
  print("========= process method starting %s =========" % str(time))
  try:
    # change the rdd to a tagged one
    #rdd = requests.post(tagger_url,data=json.dumps(rdd)).json()
    rowRdd = rdd.map(lambda w: Row(author=w['user_screen_name'], body=w['text'], created_at_utc=w['timestamp_ms'][0:9]))
    df = getSqlContextInstance(rdd.context).createDataFrame(rowRdd) 
    df.registerTempTable("df")
    df_jsons = df.toJSON()
    first = df_jsons.first()
    print(first)
    #df_json.show()
  except:
    pass
  print("========= process method ends %s =========" % str(time))



# jsons_stream = stream.map(lambda x: json.loads(x[1])) # make new stream
# jsons_stream = stream.map(tag) # make new stream
#parsed = jsons_stream.transform(tfunc_transform_data_stream)
def printRdd(x):
  print(json.dumps(x.take(2)))
  print(x)

# lines = stream.map(lambda x: json.loads(x[1]))
lines = stream.map(lambda x: requests.post(tagger_url,data=json.dumps(json.loads(x[1])) ) )
lines.foreachRDD(printRdd)

# stream = stream.transform(tfunc_transform_data_stream)
# parsed = stream.map(lambda (k,v): json.loads(v))
#stream = stream.transform(tfunc_transform_data_stream)

# parsed.foreachRDD(process)
ssc.start()
ssc.awaitTermination()

