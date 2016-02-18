# tagger web service

please see application.conf to update configs
	
	
## running 

	ssh ubuntu@

	mvn install -Dmaven.test.skip=true
	
	# please see application.conf to update configs
	
	
	# run as webservice (tagger service only)
	nohup java -Dspring.application.json='{"tagger.webservice":"true"}'   -jar target/TAG-0.0.7-SNAPSHOT.jar service  & 

<img src="http://gdurl.com/WWZq" />
	
