--CREATE KEYSPACE text_bids WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};
USE text_bids;
-- store final output
drop TABLE IF EXISTS bidswon;
CREATE TABLE bidswon (
id text, author text, body text, price double,
created_utc timestamp, symptomtags set<text>, 
conditiontags set<text>, pharmatag text, 
PRIMARY KEY ((pharmatag), created_utc) ) 
WITH CLUSTERING ORDER BY (created_utc DESC);
-- example insert
-- USE text_bids;
INSERT INTO bidswon (id, author, body, created_utc, symptomtags, conditiontags, pharmatag,price) VALUES ('hoolixyz', 'hooli', 'i have a headache', '2015-09-01 10:03:00', {'headache', 'pain'}, {'cold','flu'}, 'superPharm',0.01);
-- example selects
SELECT * FROM bidswon;
select count(*) from bidswon;
select * from bidswon where pharmatag ='superPharm' limit 5;
--------- 
drop TABLE IF EXISTS symptoms;
CREATE TABLE symptoms(
id text, created_utc timestamp, symptom text,
PRIMARY KEY ((symptom), created_utc) ) 
WITH CLUSTERING ORDER BY (created_utc DESC);
--------- 
drop TABLE IF EXISTS conditions;
CREATE TABLE conditions(
id text, created_utc timestamp, condition text,
PRIMARY KEY ((condition), created_utc) ) 
WITH CLUSTERING ORDER BY (created_utc DESC);
