package com.insightde.types.service.restful.response;

import static org.apache.log4j.Logger.getLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightde.types.TT.response.Tweet;
import com.insightde.types.reddit.Reddit;

public class TaggerResponse {
	
	private static ObjectMapper mapper = new ObjectMapper();
	private static final Logger logger = getLogger(TaggerResponse.class);
    
	private String status;
    private Map<String,Reddit> taggerResp =null;
     
    public String getStatus() {
		return status;
	}
    public void setStatus(String status) {
		this.status = status;
	}
    public List< Reddit> getTaggerResp() {
    	return new ArrayList<Reddit>(taggerResp.values());
	}

	public void setTaggerResp(Map<String, Reddit> taggerResp) {
		this.taggerResp = taggerResp;
	}

	public TaggerResponse(){
    	
    }
	
	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		List<Reddit> enrichedReddits= getTaggerResp();
		StringBuilder sb = new StringBuilder();
		int count =0;
		for(Reddit reddit: enrichedReddits){
			if(count>0)
				sb.append("\n");
			sb.append(reddit.toString());
			count+=1;
		}
		return sb.toString();
	}
	
	public static TaggerResponse jsonToPojo(String json){
		TaggerResponse tr=null;
		try {
			tr = mapper.readValue(json, TaggerResponse.class);
		} catch (JsonParseException e) {
			logger.error(e.toString());
		} catch (JsonMappingException e) {
			logger.error(e.toString());
		} catch (IOException e) {
			logger.error(e.toString());
		}
		return tr;
	}

}
