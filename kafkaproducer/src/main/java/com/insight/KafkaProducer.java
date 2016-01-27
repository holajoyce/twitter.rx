package com.insight;



import java.util.Properties;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

public class KafkaProducer {
	
	private static final Config conf = ConfigFactory.load();

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Properties props = new Properties();
		 
		props.put("metadata.broker.list", conf.getString("kafka.brokers"));
		props.put("serializer.class", "kafka.serializer.StringEncoder");
		// props.put("partitioner.class", "example.producer.SimplePartitioner");
		props.put("request.required.acks", "1");
		 
		ProducerConfig config = new ProducerConfig(props);
		
		Producer<String, String> producer = new Producer<String, String>(config);
		
		String date = "04092014" ;
		String topic = conf.getString("kafka.topic");
		
		
		for (int i = 1 ; i <= 1000 ; i++) {
			
			String msg = date + " This is message " + i ;
			System.out.println(msg) ;
			
			KeyedMessage<String, String> data = new KeyedMessage<String, String>(topic, String.valueOf(i), msg);
			 
			producer.send(data);
			
			
		}
		
		producer.close();
		

	}

}
