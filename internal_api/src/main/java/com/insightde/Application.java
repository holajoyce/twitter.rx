package com.insightde;

import java.io.IOException;
//import java.text.SimpleDateFormat;
//import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
//import com.typesafe.config.Config;
//import com.typesafe.config.ConfigFactory;

@SpringBootApplication
@Async
@EnableScheduling
public class Application {
//	private static final Config conf = ConfigFactory.load();
//	private static final String simpleDateFormatString = conf.getString("dateformat.simple");
//	private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(simpleDateFormatString);

	public Application(){
	}
	
	public static void main(String[] args) throws IOException  {
    	SpringApplication.run(Application.class, args);
    }
}
