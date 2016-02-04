# http://aimotion.blogspot.com/2013/01/machine-learning-and-data-mining.html
# http://www.datastax.com/dev/blog/big-analytics-with-r-cassandra-and-hive
# https://stackoverflow.com/questions/24272452/unable-to-connect-cassandra-through-r

# wget http://repo1.maven.org/maven2/org/apache/cassandra/cassandra-all/1.1.0/cassandra-all-1.1.0.jar
# wget http://central.maven.org/maven2/org/apache/cassandra/cassandra-clientutil/1.0.2/cassandra-clientutil-1.0.2.jar

#install.packages("RJDBC")
#install.packages("rJava")
#install.packages("DBI")

#library(RJDBC)
#library(rJava)
#library(DBI)

#cassdrv <- JDBC("org.apache.cassandra.cql.jdbc.CassandraDriver", list.files("/opt/workspace/apache-cassandra-3.2.1/lib/",pattern="jar$",full.names=T))
#.jaddClassPath("/opt/workspace/apache-cassandra-3.2.1/lib/cassandra-clientutil-3.2.1.jar")
#casscon <- dbConnect(cassdrv, "jdbc:cassandra://namenode:9160/text_bids")

#install.packages("rPython")
library(rPython)
python.exec("import sys")
python.exec("sys.path.append('/usr/local/lib/python2.7/site-packages')")
#python.exec("sys.path.append('/Library/Python/2.7/site-packages')")
python.exec("import cql")

python.exec("connection=cql.connect('localhost', cql_version='3.0.0')")
python.exec("cursor = connection.cursor()")
python.exec("cursor.execute('use text_bids')")
python.exec("cursor.execute('select * from bidswon limit 3' )")

python.exec("rep = lambda x : '__NA__' if x is None else x")
python.exec( "def getData(): return [rep(num) for line in cursor for num in line ]" )
data <- python.call("getData")
df <- as.data.frame(matrix(unlist(data), ncol=15, byrow=T))
