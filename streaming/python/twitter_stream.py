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


kafkaStream = KafkaUtils.createStream(ssc, "ip-172-31-1-136:2181,ip-172-31-1-137:2181,ip-172-31-1-133:2181,ip-172-31-1-139:2181", "spark-streaming consumer", {"reddit4": 1})
lines = kafkaStream.map(lambda x: x[1])

# {"name":"t1_c8770z6","author_flair_css_class":null,"author":"corey3","author_flair_text":null,"ups":1,"id":"c8770z6","edited":false,"retrieved_on":1431145410,"score_hidden":false,"gilded":0,"downs":0,"body":"also try to get ADHD medication.","controversiality":0,"subreddit_id":"t5_2qnwb","distinguished":null,"parent_id":"t1_c85yez1","subreddit":"ADHD","archived":true,"score":1,"link_id":"t3_17itl1"}
counts = lines.flatMap(lambda line: line.split(" ")) \
        .map(lambda word: (word, 1)) \
        .reduceByKey(lambda a, b: a+b)
counts.pprint()

ssc.start()
ssc.awaitTermination()