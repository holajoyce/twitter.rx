
from pyspark.streaming.kafka import KafkaUtils
from pyspark.sql.types import *  
from pyspark.sql import SQLContext

sqlContext = SQLContext(sc)
ssc = StreamingContext(sc, 2)

reddit = sqlContext.read.json("s3n://reddit-comments/2007/RC_2007-10").registerTempTable("reddit")
reddit.take(2)
#a_user = sqlContext.sql("SELECT * from reddit WHERE author ='bostich'")

new_reddit = reddit.select( expr("id as id"),\
    expr("trim(body) AS text"), \
    expr("author AS user_screen_name"), \
    expr("created_utc AS created_utc")\
).show()


# directKafkaStream = KafkaUtils.createDirectStream(ssc, "TT_raw", {"metadata.broker.list": brokers})
kafka_stream = KafkaUtils.createStream(scc, \
	"ec2-52-35-9-210.us-west-2.compute.amazonaws.com:2181,ec2-52-33-29-117.us-west-2.compute.amazonaws.com:2181,ec2-52-88-208-18.us-west-2.compute.amazonaws.com:2181,ec2-52-33-248-41.us-west-2.compute.amazonaws.com:28181", \
	"TT_raw",data)

# send to kafka


twitterDf = sqlContext.jsonFile("s3n://joyce-raw-twitter/TT-logs8/events/ts=20160126-17/events_0.json")
twitterDf.persist(StorageLevel.MEMORY_AND_DISK_SER)
twitterDf.registerTempTable("twitterDf")
twitterDf.printSchema()

a_user = sqlContext.sql("SELECT * from twitterDf WHERE user_screen_name ='juliejones88801'")

hashtags = sqlContext.sql("SELECT hashtags from twitterDf")


# schemaString = "message user_screen_name"
# fields = [StructField(field_name, StringType(), True) for field_name in schemaString.split()]
# schema = StructType(fields)

# # Apply the schema to the RDD.
# schemaT = sqlContext.createDataFrame(twitterRdd, schema)

# distFile.map(lambda s: len(s)).reduce(lambda a, b: a + b).






