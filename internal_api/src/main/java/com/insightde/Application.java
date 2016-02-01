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
	// {"created_at":"Sun Jan 31 20:49:42 +0000 2016","id":693898976458903552,"id_str":"693898976458903552","text":"I feel it coming out my throat, guess I better wash my mouth out with soap","source":"<a href=\"http://twitter.com/download/iphone\" rel=\"nofollow\">Twitter for iPhone</a>","truncated":false,"in_reply_to_status_id":null,"in_reply_to_status_id_str":null,"in_reply_to_user_id":null,"in_reply_to_user_id_str":null,"in_reply_to_screen_name":null,"user_id":462388397,"user_id_str":"462388397","user_name":"16.","user_screen_name":"YOUTUBEARl","user_location":"yt 5sos sel tay ari lana tvd","user_url":"http://sweeetsexual.tumblr.com/findme","user_description":"lust and ardor. ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀✨ favim: lukehemmings⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀✨","user_protected":false,"user_verified":false,"user_followers_count":14139,"user_friends_count":1526,"user_listed_count":171,"user_favourites_count":78987,"user_statuses_count":119951,"user_created_at":"Thu Jan 12 22:23:14 +0000 2012","user_utc_offset":-18000,"user_time_zone":"America/Detroit","user_geo_enabled":true,"user_lang":"en","user_contributors_enabled":false,"user_is_translator":false,"user_profile_background_color":"696969","user_profile_background_image_url":"http://pbs.twimg.com/profile_background_images/561273109496360962/SEVtx1WA.jpeg","user_profile_background_image_url_https":"https://pbs.twimg.com/profile_background_images/561273109496360962/SEVtx1WA.jpeg","user_profile_background_tile":false,"user_profile_link_color":"3D3D3D","user_profile_sidebar_border_color":"FFFFFF","user_profile_sidebar_fill_color":"99CC33","user_profile_text_color":"EB9BC3","user_profile_use_background_image":false,"user_profile_image_url":"http://pbs.twimg.com/profile_images/693297889859231744/QHgtGLcF_normal.jpg","user_profile_image_url_https":"https://pbs.twimg.com/profile_images/693297889859231744/QHgtGLcF_normal.jpg","user_profile_banner_url":"https://pbs.twimg.com/profile_banners/462388397/1454113415","user_default_profile":false,"user_default_profile_image":false,"user_following":null,"user_follow_request_sent":null,"user_notifications":null,"geo":null,"coordinates":null,"place":null,"contributors":null,"is_quote_status":false,"retweet_count":0,"favorite_count":0,"entities_hashtags":[],"entities_urls":[],"entities_user_mentions":[],"entities_symbols":[],"favorited":false,"retweeted":false,"filter_level":"low","lang":"en","timestamp_ms":"1454273382664"}
	// {"subreddit_id":"t5_2yljs","score":0,"edited":false,"name":"t1_cekta7f","author_flair_css_class":"default","author":"Kimera25","parent_id":"t1_ceks74z","link_id":"t3_1uqrin","retrieved_on":1431859483,"score_hidden":false,"author_flair_text":"360 100%","subreddit":"chiliadmystery","downs":0,"removal_reason":null,"controversiality":0,"id":"cekta7f","ups":0,"gilded":0,"distinguished":null,"body":"I just tried it and nothing, it's a half moon on a tuesday, no rain, i'll try it with a thunderstorm next time. this got me thinking that the mural could have been painted by the Altruists and each of the X's represents a sacrifice and you have to do the fifth one, I guess on chop. random theory built off all this.","archived":true}
}
