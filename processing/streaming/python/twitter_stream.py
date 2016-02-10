from __future__ import print_function

import sys

from pyspark import SparkContext, SparkConf
from pyspark.streaming import StreamingContext
from pyspark.streaming.kafka import KafkaUtils
from pyspark.sql import SQLContext, Row
from pyspark.sql.functions import explode
from pyspark.sql.functions import from_unixtime
# from pyspark.sql.DataFrame import alias
import json
import requests
from time import gmtime, strftime
import unicodedata,re
from pyspark_elastic import EsSparkContext
from kafka import KafkaProducer

from fluent import sender
from fluent import event

# REFERENCES
# https://github.com/apache/spark/blob/master/examples/src/main/python/streaming/sql_network_wordcount.py
# https://github.com/willzfarmer/TwitterPanic/blob/master/python/analysis.py
# https://raw.githubusercontent.com/rustyrazorblade/killranalytics/intro_streaming_python2/killranalytics/spark/raw_event_stream_processing.py
# https://rideondata.wordpress.com/2015/06/29/analyzing-wikipedia-text-with-pyspark/
# https://github.com/willzfarmer/TwitterPanic/tree/master/python
# https://docs.cloud.databricks.com/docs/latest/databricks_guide/08%20Spark%20Streaming/06%20FileStream%20Word%20Count%20-%20Python.html

# multiple d-streams (see tDas response)
# http://apache-spark-user-list.1001560.n3.nabble.com/using-multiple-dstreams-together-spark-streaming-td9947.html

# tabular data
# https://www.mapr.com/blog/using-apache-spark-dataframes-processing-tabular-data

# joining dataframes (see section, a more concrete example)
# https://spark.apache.org/docs/1.5.2/api/python/pyspark.sql.html

# how not to duplicate columns
# https://forums.databricks.com/questions/876/is-there-a-better-method-to-join-two-dataframes-an.html

# cassandra
# http://rustyrazorblade.com/2015/08/migrating-from-mysql-to-cassandra-using-spark/

# to kafka
# https://stackoverflow.com/questions/32320618/sending-large-csv-to-kafka-using-python-spark

# if need to use connectionpool http://www.cnblogs.com/englefly/p/4579863.html

BATCH_INTERVAL = 1.2;
def getSqlContextInstance(sparkContext):
  if ('sqlContextSingletonInstance' not in globals()):
      globals()['sqlContextSingletonInstance'] = SQLContext(sparkContext)
  return globals()['sqlContextSingletonInstance']

conf = SparkConf().setAppName("PySpark Cassandra Text Bids Join").set("spark.es.host", "ec2-52-88-7-3.us-west-2.compute.amazonaws.com").set("spark.streaming.receiver.maxRate",2000).set("spark.streaming.kafka.maxRatePerPartition",1000).set("spark.streaming.backpressure.enabled",True).set("spark.cassandra.connection.host","172.31.1.138")

kafkaBrokers = {"metadata.broker.list": "52.33.29.117:9092,52.33.248.41:9092,52.35.99.109:9092,52.89.231.174:9092"}

sc = SparkContext(conf=conf)
ssc = StreamingContext(sc, BATCH_INTERVAL)

sender.setup('spark.out', host='localhost', port=24224)

zkQuorum, topic = sys.argv[1:]

# streams = [KafkaUtils.createStream(ssc, zkQuorum, "spark-streaming-consumer", {topic: 1}) for x in range(0,3)]
# stream = ssc.union(streams)  
# stream = KafkaUtils.createStream(ssc, zkQuorum, "spark-streaming-consumer", {topic: 1})
stream = KafkaUtils.createDirectStream(ssc, [topic], kafkaBrokers)

# streams2 = [KafkaUtils.createStream(ssc, zkQuorum, "spark-streaming-consumer2", {"pharma_bids_prices2": 1}) for x in range(0,3)]
# stream2 = ssc.union(streams2)
# stream2 = KafkaUtils.createStream(ssc, zkQuorum, "spark-streaming-consumer2", {"pharma_bids_prices2": 1}) 
stream2 = KafkaUtils.createDirectStream(ssc, ["pharma_bids_prices2"], kafkaBrokers)


datasourcetype = "TT" if topic=="TT_raw" else "RD"
tagger_url = "http://localhost:8555/tagbatch/"+datasourcetype

lines_count =0;
LINES_COUNT_MAX=100;

control_chars = ''.join(map(unichr, range(0,32) + range(127,160)))
control_char_re = re.compile('[%s]' % re.escape(control_chars))


def process(rdd):
  print(">>>> BEGIN CASS")
  wonbids = getSqlContextInstance(rdd.context).createDataFrame(rdd) 
  wonbids.registerTempTable("wonbids")
  wonbids.write.format("org.apache.spark.sql.cassandra").\
           options(keyspace="text_bids", table="bidswon").\
           save(mode="append")
  #sqlContext.cacheTable('wonbids')
  # wonbids.show()
 
  symptoms = wonbids.select(wonbids.id,wonbids.created_utc,explode(wonbids.symptomtags).alias('symptom'))
  symptoms.registerTempTable("symptoms")
  symptoms.write.format("org.apache.spark.sql.cassandra").\
         options(keyspace="text_bids", table="symptoms").\
         save(mode="append")
  # symptoms.show()

  conditions = wonbids.select(wonbids.id,wonbids.created_utc,explode(wonbids.conditiontags).alias('condition'))
  conditions.registerTempTable("conditions")
  conditions.write.format("org.apache.spark.sql.cassandra").\
         options(keyspace="text_bids", table="conditions").\
         save(mode="append")
  # conditions.show()
  
  # send back to master to process
  for w in wonbids.collect():
    event.Event('toES', {'id':w.id,'pharmatag':w.pharmatag,'price':w.price,'created_utc':w.created_utc,'symptomtags':w.symptomtags,'conditiontags':w.conditiontags})
  print(">>>> END CASS")


def debugprint(rdd):
  print(rdd.take(1))

def debugprintjson(rdd):
  print(json.dumps(rdd.take(1)))

# this function converts rdds into dataframes & join & filter, and return back rdd
def tfunc(t,rdd,rddb):
  # texts
  try:
    #----- texts
    if topic=="TT_raw":
      rowRdd = rdd.map(lambda w: Row(id=w['id'],author=w['user_screen_name'],\
      body=w['body'], created_utc=str(int(w['timestamp_ms'])/1000), \
      pharmatags=w['pharmatags'],conditiontags=w['conditiontags'], symptomtags=w['symptomtags']))
    else:  # this is reddit
      rowRdd = rdd.map(lambda w: Row(id=w['id'],author=w['author'],\
      body=w['body'], created_utc=w['created_utc'], \
      pharmatags=w['pharmatags'],conditiontags=w['conditiontags'], symptomtags=w['symptomtags']))

    texts = getSqlContextInstance(rdd.context).createDataFrame(rowRdd) 
    texts.registerTempTable("texts")
    texts = texts.select(texts.id,from_unixtime(texts.created_utc).alias('created_utc'),texts.author,texts.body, explode(texts.pharmatags).alias('pharmatag'), texts.conditiontags, texts.symptomtags)
    # return texts.rdd

    #----- bids
    rowRdd2= rddb.map(lambda w: Row(price=w['price'], pharmatag=w['pharmatags']))
    bids = getSqlContextInstance(rddb.context).createDataFrame(rowRdd2) 
    bids.registerTempTable("bids")
    getSqlContextInstance(rdd.context).cacheTable('bids')
    bids = bids.select(bids.price,bids.pharmatag)
    
    # #---- texts ids joined with pharma bids, java webservice already sorted by price
    idbids = bids.join(texts,texts.pharmatag==bids.pharmatag,'inner').select(texts.id,texts.author, texts.created_utc, texts.body, texts.conditiontags, texts.symptomtags, bids.pharmatag,bids.price).limit(1)
    idbids.registerTempTable("idbids")
    idbids.show()
    return idbids.rdd


    # #-----texts id & bids, find min
    # DEPRECATED, will just return the top matched, since it's already sorted by price  by java service
    # idsbidsmin = getSqlContextInstance(rddb.context).sql("SELECT id, author, created_utc, body, pharmatag, conditiontags, symptomtags, max(price) as price FROM idbids GROUP BY id,author, created_utc, body, conditiontags, symptomtags, pharmatag ")
    # idsbidsmin.registerTempTable("idsbidsmin") # dataframe
    # idsbidsmin.show()
    # return idsbidsmin.rdd

  except 'Exception':
    pass
#------------
# get 2 different streams: 1 for texts (after being tagged by webservice), the other bids from pharmaceutical companies
# lines_texts = stream.map(lambda x: requests.post(tagger_url,data=json.dumps(json.loads( control_char_re.sub('',x[1]))) ).json() ).filter(lambda w: w is not None and "pharmatags" in w and len(w['pharmatags']) > 0 and "body" in w)
lines_texts = stream.map(lambda x: requests.post(tagger_url,data=json.dumps(json.loads( control_char_re.sub('',x[1]))) ).json() ).filter(lambda w: w is not None and "pharmatags" in w and len(w['pharmatags']) > 0 and "body" in w)
lines_bids = stream2.map(lambda x: json.loads(x[1])).filter(lambda w: w is not None) 
# lines_bids.foreachRDD(debugprintjson)  

# join the streams together
lines_texts_with_bids = lines_texts.transformWith(tfunc, lines_bids)

# get back a new type of rdd & process
lines_texts_with_bids.foreachRDD(process) 

# if want to see output, can use
# lines_texts_with_bids.pprint()

ssc.start()
ssc.awaitTermination()

