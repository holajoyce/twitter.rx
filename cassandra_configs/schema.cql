CREATE KEYSPACE text_bids WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};
USE text_bids;

--final output
CREATE TABLE bidswon (
id text, author text, body text, 
created_utc timestamp, symptomstags set<text>, 
conditiontags set<text>, pharmatag text, 
PRIMARY KEY ((pharmatag), created_utc), ) 
WITH CLUSTERING ORDER BY (created_utc DESC);
