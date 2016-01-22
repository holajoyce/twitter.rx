[slides 50.112.137.240:1948/slide_deck.md](http://50.112.137.240:1948/slide_deck.md)

# -- cluster 1 (yellow)

joyce-kafak3 contains 
- zookeeper, (running)
- kafaka,  (running)
- elasticsearch (stopped)

ec2-52-35-9-210.us-west-2.compute.amazonaws.com:9200/_cat/nodes?v

#-- cluster 2 (red)

joyce-cassandra  is 3 nodes has 

- 3 nodes of only cassandra (running)
- the m4.large has collectors running also in fluentd (running)
- m4.large will also have flask (not installed yet)


#--- cluster 3 (blue)

joyce-proc5 has

(all running)
spark 
spark master
hadoop 
hadoop namenode
