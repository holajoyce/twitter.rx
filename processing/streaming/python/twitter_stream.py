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

spark-submit --packages org.apache.spark:spark-streaming-kafka_2.10:1.5.1,TargetHolding:pyspark-cassandra:0.2.7 --master spark://ip-172-31-1-134:7077  --executor-memory 10G --driver-memory 10G twitter_stream.py "ec2-52-33-248-41.us-west-2.compute.amazonaws.com:2181" "TT_raw"
"""

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
def getSqlContextInstance(sparkContext):
  if ('sqlContextSingletonInstance' not in globals()):
      globals()['sqlContextSingletonInstance'] = SQLContext(sparkContext)
  return globals()['sqlContextSingletonInstance']

conf = SparkConf().setAppName("PySpark Cassandra Text Bids Join").set("spark.es.host", "ec2-52-88-7-3.us-west-2.compute.amazonaws.com")


# sc = CassandraSparkContext(conf=conf)
sc = SparkContext(conf=conf)
# sc = EsSparkContext(conf=conf)
ssc = StreamingContext(sc, 2)

sender.setup('spark.out', host='localhost', port=24224)

zkQuorum, topic = sys.argv[1:]
stream = KafkaUtils.createStream(ssc, zkQuorum, "spark-streaming-consumer", {topic: 1})
stream2 = KafkaUtils.createStream(ssc, zkQuorum, "spark-streaming-consumer2", {"pharma_bids_prices2": 1})
# producer = KafkaProducer(value_serializer=json.loads,bootstrap_servers=['ec2-52-33-248-41.us-west-2.compute.amazonaws.com:9092'])

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
  wonbids.show()
 
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

  # this is a smaller set
  # wonbids_less_txt = wonbids.select(wonbids.id, wonbids.created_utc, wonbids.symptomtags, wonbids.conditiontags)
  # wonbidsJsons = wonbids_less_txt.toJSON()
  #wonbids_less_txt.registerTempTable("wonbids_less_txt")

  # send back to master to process
  for w in wonbids.collect():
    event.Event('toES', {'id':w.id,'pharmatag':w.pharmatag,'price':w.price,'created_utc':w.created_utc,'symptomtags':w.symptomtags,'conditiontags':w.conditiontags})
    # event.Event('toES', {'id':w.id,'author':w.author,'body':w.body,'pharmatag':w.pharmatag,'price':w.price,'created_utc':w.created_utc,'symptomtags':w.symptomtags,'conditiontags':w.conditiontags})

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
    bids = bids.select(bids.price,bids.pharmatag)
    
    # #---- texts ids joined with pharma bids
    idbids = texts.join(bids,texts.pharmatag==bids.pharmatag,'inner').select(texts.id,texts.author, texts.created_utc, texts.body, texts.conditiontags, texts.symptomtags, bids.pharmatag,bids.price)
    idbids.registerTempTable("idbids")

    # #-----texts id & bids, find min
    idsbidsmin = getSqlContextInstance(rddb.context).sql("SELECT id, author, created_utc, body, pharmatag, conditiontags, symptomtags, max(price) as price FROM idbids GROUP BY id,author, created_utc, body, conditiontags, symptomtags, pharmatag ")
    idsbidsmin.registerTempTable("idsbidsmin") # dataframe

    # idsbidsmin.show()
    return idsbidsmin.rdd

  except 'Exception':
    pass
#------------
# get 2 different streams: 1 for texts (after being tagged by webservice), the other bids from pharmaceutical companies
lines_texts = stream.map(lambda x: requests.post(tagger_url,data=json.dumps(json.loads( control_char_re.sub('',x[1]))) ).json() ).filter(lambda w: w is not None and "pharmatags" in w and len(w['pharmatags']) > 0 and "body" in w)
lines_bids = stream2.map(lambda x: json.loads(x[1])).filter(lambda w: w is not None) 
# lines_bids.foreachRDD(debugprintjson)  

# join the streams together
lines_texts_with_bids = lines_texts.transformWith(tfunc, lines_bids)

# get back a new type of rdd & process
lines_texts_with_bids.foreachRDD(process) 
# lines_texts_with_bids.foreachPartition(process)

# if want to see output, can use
# lines_texts_with_bids.pprint()
# lines_texts_with_bids.mapPartitions(process)
# lines_texts_with_bids.foreachRDD(process)

ssc.start()
ssc.awaitTermination()

"""
[Row(id=694280251245879296, author=u'taramcgarv', created_utc=14543642851, body=u'RT @ryanxmailloux: dating someone who is also your bestfriend is the best relationship', pharmatag=u'abbvie_inc', conditiontags=[u'Psoriasis', u'Hyperthyroidism', u'Migraine', u'Gout', u'Pancreatitis', u'Preterm labor', u"Athlete's foot", u'Breast cancer', u'Arthritis', u'Hives', u'Gonorrhea', u'Gastritis', u'GERD', u'Urinary tract infection', u'Chlamydia', u'Syphilis', u'Obsessive-compulsive disorder', u'Tetanus', u'BPH', u'Hemorrhoids', u"Bell's palsy"], symptomtags=[u'Bright red blood covering your stool, on toilet paper, or in the toilet bowl', u'Goiter (an enlarged thyroid that may cause your neck to look swollen)', u'Discharge from your vagina or penis', u'Dyspepsia (a painful gnawing sensation in your stomach)', u'A change in the size or shape of your breast', u'Painful tightening of the muscles, usually all over your body', u'A lump in your breast', u'Sudden facial paralysis, usually on one side of your face', u'Red, itchy bumps on your skin', u'Aura: Flashing lights or zigzagging lines in your vision', u'Taste of stomach fluid in the back of your mouth', u'Usually affects your big toe first, but can also affect ankles, heels, knees, wrists, fingers, and elbows', u'Small amounts of blood in your urine', u'Pain in your back or side below the ribs', u'Abdominal pain that radiates to your back', u'Reduced motion in your joints', u'Men: Pain when urinating, discharge from your penis, problems with the prostate or testicles', u'Aching in your lower back', u'Third stage: Soft masses of inflammation called gummas anywhere in your body; neurological damage that can cause pain, problems with balance, and meningitis; and damage to your cardiovascular system', u'Cracked, scaly skin between your toes', u'Repeated rituals and behaviors, called compulsions, to calm your thoughts', u'Patches usually appear on elbows, knees, scalp, back, face, palms, and feet, but they can show up on other parts of your body', u'Pressure in your lower belly', u'Can occur in any joint, but usually affects your hands, knees, hips, or spine'], price=0.05), Row(id=694280976898351104, author=u'alexrntrimble', created_utc=1454364458, body=u"Tryin to stay busy enough that I can't panic about things which is resulting in panicking about being busy instead. uuugh.", pharmatag=u'abbvie_inc', conditiontags=[u'Lyme disease', u'Insomnia', u'Schizophrenia', u'Anxiety', u'Dementia'], symptomtags=[u"Positive symptoms: Hallucinations (hearing or seeing things that aren't there), delusions (beliefs that aren't true), trouble organizing thoughts, and strange movements", u'Difficulty thinking well enough to do normal activities, such as getting dressed or eating', u'Worrying about worrying', u'A rash, which may look like a bullseye', u'Being awake for much of the night'], price=0.05), Row(id=694280880412446720, author=u'fIuffroses', created_utc=1454364435, body=u'RT @tomlinsonshine: no one close to the birthday babe ever calls him by his real name cutee https://t.co/Nh7PtWtK3k', pharmatag=u'abbvie_inc', conditiontags=[u'Pulmonary embolism', u'Stroke', u'Migraine', u"Bell's palsy"], symptomtags=[u'Throbbing or pulsing headache, usually on one side of the head', u'Warmth, swelling, or redness of one leg', u'Sudden numbness or weakness of the face, arm, or leg (especially on one side of the body)', u'Sudden trouble seeing in one or both eyes', u'Sudden facial paralysis, usually on one side of your face'], price=0.05), Row(id=694281647953326080, author=u'aleeokj', created_utc=1454364618, body=u"RT @RedHairPolitics: Not sure if I'm single because nobody likes me or because I like nobody...", pharmatag=u'abbvie_inc', conditiontags=[u'Lyme disease', u'Malaria', u'HIV', u'Syphilis', u'Impetigo', u'Post-traumatic stress disorder', u'Narcolepsy', u'Hepatitis C'], symptomtags=[u'First stage: A single, small, painless genital sore', u'Flu-like symptoms', u'Red or pimple-like sores surrounded by red skin', u'Cataplexy: Sudden, temporary muscle weakness or paralysis caused by strong emotions like anger, laughter, and surprise', u'A rash, which may look like a bullseye', u'Flashbacks, or feeling like the traumatic event is happening again'], price=0.05), Row(id=694281920583221250, author=u'jpgravely', created_utc=1454364683, body=u'RT @RAIDERS: It has been an honor having you in the Silver and Black, @JustinTuck. #Raiders https://t.co/LOQ6ZbqVSW', pharmatag=u'abbvie_inc', conditiontags=[u'Pneumonia', u'Insomnia', u'Anthrax', u'Fibromyalgia', u'Depression', u'Narcolepsy', u'High blood pressure', u'BPH'], symptomtags=[u'Loss of interest or pleasure in activities you used to enjoy', u'Tender points on the neck, shoulders, back, hips, arms, and legs that hurt more when you put pressure on them', u'Lying awake for a long time before you fall asleep', u'The feeling that you have to go, even just after urinating', u'Sleep paralysis, or the inability to move while you fall asleep or wake up', u'Chest pain when you breathe or cough', u"Feeling as if you haven't slept at all", u'Hallucinations while you fall asleep or wake up', u'Cutaneous (skin) form: Small, painless, but sometimes itchy skin lesions on exposed areas such as the face, neck, arms, and hards. The lesions develop into blisters and then ulcers with a black center. Other symptoms include fever, malaise, headache, and major organ problems.', u'Usually has no symptoms but can cause stroke, heart failure, or heart attack'], price=0.05), Row(id=694280314198183937, author=u'illl_billl', created_utc=1454364300, body=u"Hey Niko, it's your cousin, do you want to go bowling?", pharmatag=u'abbvie_inc', conditiontags=[u'Pneumonia', u'Gout', u'Fibromyalgia', u"Athlete's foot", u'Arthritis', u'Hives', u'Urinary tract infection', u'Chlamydia', u'Syphilis', u'Narcolepsy', u'Obsessive-compulsive disorder', u'Anxiety', u'Psoriasis', u'Migraine', u'Hyperthyroidism', u'Pancreatitis', u'Preterm labor', u'Depression', u'Breast cancer', u'Gonorrhea', u'Gastritis', u'GERD', u'Insomnia', u'Tetanus', u'BPH', u'Hemorrhoids', u'Borderline personality disorder', u"Bell's palsy", u'Dementia'], symptomtags=[u'Bright red blood covering your stool, on toilet paper, or in the toilet bowl', u'Discharge from your vagina or penis', u'Painful tightening of the muscles, usually all over your body', u"Feeling as if you haven't slept at all", u'Aura: Flashing lights or zigzagging lines in your vision', u'Loss of interest or pleasure in activities you used to enjoy', u'Taste of stomach fluid in the back of your mouth', u'Usually affects your big toe first, but can also affect ankles, heels, knees, wrists, fingers, and elbows', u'Lying awake for a long time before you fall asleep', u'Pain in your back or side below the ribs', u'Reduced motion in your joints', u'Men: Pain when urinating, discharge from your penis, problems with the prostate or testicles', u'Sleep paralysis, or the inability to move while you fall asleep or wake up', u'Chest pain when you breathe or cough', u'Repeated rituals and behaviors, called compulsions, to calm your thoughts', u'Patches usually appear on elbows, knees, scalp, back, face, palms, and feet, but they can show up on other parts of your body', u'Inability to let go of a worry', u'Extreme emotions that can go back and forth very quickly', u'Can occur in any joint, but usually affects your hands, knees, hips, or spine', u'Goiter (an enlarged thyroid that may cause your neck to look swollen)', u'Dyspepsia (a painful gnawing sensation in your stomach)', u'The feeling that you have to go, even just after urinating', u'A change in the size or shape of your breast', u'A lump in your breast', u'Sudden facial paralysis, usually on one side of your face', u'Red, itchy bumps on your skin', u'Small amounts of blood in your urine', u'Tender points on the neck, shoulders, back, hips, arms, and legs that hurt more when you put pressure on them', u'Abdominal pain that radiates to your back', u'Aching in your lower back', u'Third stage: Soft masses of inflammation called gummas anywhere in your body; neurological damage that can cause pain, problems with balance, and meningitis; and damage to your cardiovascular system', u'Difficulty thinking well enough to do normal activities, such as getting dressed or eating', u'Cracked, scaly skin between your toes', u'Hallucinations while you fall asleep or wake up', u'Pressure in your lower belly'], price=0.05), Row(id=694281585042919424, author=u'SeigfriednoRoy', created_utc=1454364603, body=u"On the bright side... It's after 5 and still light outside. #FoodForThought #TheMoreYouKnow", pharmatag=u'abbvie_inc', conditiontags=[u'Glaucoma', u'Stroke', u'Urinary tract infection', u'Pneumonia', u'HIV', u'Migraine', u'Kidney infection', u'Shingles', u'BPH', u'Hemorrhoids', u"Bell's palsy"], symptomtags=[u'Bright red blood covering your stool, on toilet paper, or in the toilet bowl', u'Throbbing or pulsing headache, usually on one side of the head', u'Burning or shooting pain and tingling or itching usually on once side of the body or face', u'Sudden numbness or weakness of the face, arm, or leg (especially on one side of the body)', u'Pain in your back or side below the ribs', u'The feeling that you have to go, even just after urinating', u'Sensitivity to light and sound', u'More severe symptoms, including opportunistic infections, after months or years without treatment', u'Sudden facial paralysis, usually on one side of your face', u'Feeling suddenly worse after a cold or flu', u'Slow loss of peripheral (side) vision', u'Back or side pain'], price=0.05), Row(id=694280851056693248, author=u'amberlynn543', created_utc=1454364428, body=u'MOOD https://t.co/6KWusF7Q0n', pharmatag=u'abbvie_inc', conditiontags=[u'Hyperthyroidism', u'Bipolar disorder'], symptomtags=[u'Mood changes', u'Mood swings'], price=0.05), Row(id=694280578418372608, author=u'Blackndrpacific', created_utc=1454364363, body=u"I'm so happy to be leaving this apartment soon. New home, fresh start\U0001f33b", pharmatag=u'abbvie_inc', conditiontags=[u'Bipolar disorder'], symptomtags=[u'Mania: Feeling "up", happy, active, or irritable'], price=0.05), Row(id=694281710876385281, author=u'harryscloudz', created_utc=1454364633, body=u'RT @INTERNETG0ALS: "not only did I meet my best friend but we saw the people who brought us together side by side" http://t.co/9rdVqfrLIb', pharmatag=u'abbvie_inc', conditiontags=[u'Glaucoma', u'Stroke', u'Urinary tract infection', u'Psoriasis', u'Insomnia', u'Migraine', u'Kidney infection', u'Shingles', u"Bell's palsy"], symptomtags=[u'Throbbing or pulsing headache, usually on one side of the head', u'Burning or shooting pain and tingling or itching usually on once side of the body or face', u'Sleeping for only short periods', u'Some people get joint pain and swelling called "psoriatic arthritis"', u'Sudden numbness or weakness of the face, arm, or leg (especially on one side of the body)', u'Pain in your back or side below the ribs', u'Sudden facial paralysis, usually on one side of your face', u'Slow loss of peripheral (side) vision', u'Back or side pain'], price=0.05)]
"""
