
from pyspark.sql import SQLContext
sqlContext = SQLContext(sc)
from pyspark.sql.types import *  



reddit = sqlContext.read.json("s3n://reddit-comments/2007/RC_2007-10").registerTempTable("reddit")
reddit.take(2)
a_user = sqlContext.sql("SELECT * from reddit WHERE author ='bostich'")



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






