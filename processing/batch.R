# http://aimotion.blogspot.com/2013/01/machine-learning-and-data-mining.html
# http://www.datastax.com/dev/blog/big-analytics-with-r-cassandra-and-hive
# https://stackoverflow.com/questions/24272452/unable-to-connect-cassandra-through-r

# wget http://repo1.maven.org/maven2/org/apache/cassandra/cassandra-all/1.1.0/cassandra-all-1.1.0.jar
# wget http://central.maven.org/maven2/org/apache/cassandra/cassandra-clientutil/1.0.2/cassandra-clientutil-1.0.2.jar

#install.packages("rJava")
#install.packages("RJDBC")

#install.packages("DBI")
# https://stackoverflow.com/questions/12872699/error-unable-to-load-installed-packages-just-now

#--- java way  
# sudo R CMD javareconf 
library(RJDBC)
library(rJava)
library(DBI)

cassdrv <- JDBC("org.apache.cassandra.cql.jdbc.CassandraDriver", list.files("/usr/local/cassandra/lib/",pattern="jar$",full.names=T))
#.jaddClassPath("/usr/local/cassandra/lib/cassandra-clientutil-3.2.1.jar")
casscon <- dbConnect(cassdrv, "jdbc:cassandra://localhost:9160/text_bids")


