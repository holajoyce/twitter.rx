package com.insightde.types.reddit;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.insightde.types.GenericPost;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Reddit implements GenericPost{
	private static final Logger logger = LoggerFactory.getLogger(Reddit.class);
	
	private static ObjectMapper mapper = new ObjectMapper();

	private static Config conf = ConfigFactory.load();
	private static final SimpleDateFormat logstashDateFormat = new SimpleDateFormat(conf.getString("dateformat.logstash")); //'%Y-%m-%dT%H:%M:%S%z'

	private final static String textFormat = conf.getString("text.format");

	// Constants

	// Member Variables
	private Long created_utc;
	private String parent_id;
	private String subreddit_id;
	private String id;
	private String author;
	private String body;
	private List<String> phramatags = Lists.newArrayList();
	private List<String> symptomtags = Lists.newArrayList();
	
	public static Reddit jsonToPojo(String msg) {
		mapper = new ObjectMapper();
		try {
			Reddit reddit = mapper.readValue(msg, Reddit.class);
			return reddit;
		} catch (IOException e) {
			logger.info(e.getMessage());
		}
		return null;
	}
	
	

	public String getParent_id() {
		return parent_id;
	}

	public void setParent_id(String parent_id) {
		this.parent_id = parent_id;
	}

	public String getSubreddit_id() {
		return subreddit_id;
	}

	public void setSubreddit_id(String subreddit_id) {
		this.subreddit_id = subreddit_id;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}


	public List<String> getPharmatags() {
		return phramatags;
	}

	public void setPharmatags(List<String> pharmatags) {
		this.phramatags = pharmatags;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Long getCreated_utc() {
		return created_utc;
	}

	public void setCreated_utc(Long created_utc) {
		this.created_utc = created_utc;
	}

	public List<String> getSymptomtags() {
		return symptomtags;
	}

	public void setSymptomtags(List<String> symptomtags) {
		this.symptomtags = symptomtags;
	}
	public String getIdAsStr() {
		return id;
	}
	
	@Override
	public String toString(){
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		String jsonInString="";
		try {
			jsonInString = mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
		}
		return jsonInString;
	}
}
