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

// test into {"id":"1234","text":"I have a headache","created_at": "Tue Jan 26 00:02:05 +0000 2016"}

/*

{"subreddit_id":"t5_2yljs","score":0,"edited":false,"name":"t1_cekta7f","author_flair_css_class":"default","author":"Kimera25","parent_id":"t1_ceks74z","link_id":"t3_1uqrin","retrieved_on":1431859483,"score_hidden":false,"author_flair_text":"360 100%","subreddit":"chiliadmystery","downs":0,"removal_reason":null,"controversiality":0,"id":"cekta7f","ups":0,"gilded":0,"distinguished":null,"body":"I just tried it and nothing, it's a half moon on a tuesday, no rain, i'll try it with a thunderstorm next time. this got me thinking that the mural could have been painted by the Altruists and each of the X's represents a sacrifice and you have to do the fifth one, I guess on chop. random theory built off all this.","archived":true}
*/