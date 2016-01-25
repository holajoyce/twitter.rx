package com.insightde.service.restful;

import static org.apache.log4j.Logger.getLogger;

import java.io.IOException;
//import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Maps;
import com.insightde.ApplicationModeType;
import com.insightde.taggers.services.Tagger;
import com.insightde.taggers.services.impl.ExperimentalTagger;
import com.insightde.types.TT.response.Tweet;
import com.insightde.types.service.restful.response.TaggerResponse;
import com.insightde.types.sources.DataSourceType;
import com.insightde.utils.IodineJsonParser;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@RestController
@ConditionalOnProperty(name="tagger.webservice")
public class TaggerController {
	
	
	final static org.apache.log4j.Logger logger = getLogger(TaggerController.class);
    private static Config conf = ConfigFactory.load();

	public static final ApplicationModeType applicationMode = ApplicationModeType.findByShortName(conf.getString("application.mode"));
	public static final String applicationModeName = applicationMode.getLongname();
	
	private  Tagger tagger ;
	private IodineJsonParser ijp;
	
	public TaggerController(){

		tagger =new ExperimentalTagger(
				applicationMode,
				conf.getString("tagging.dictionary.names"),
				conf.getString("tagging.pdmf.files.directory."+applicationModeName)
		);
		tagger.setDatasourceType(DataSourceType.TT);
	}

	@RequestMapping(value = "/tagbatch", method = RequestMethod.POST)
	public String process(
			@RequestParam(value="datasource",required=false) String datasource,
			@RequestBody String jsonString
	) throws JsonParseException, JsonMappingException, IOException{
		Tweet tt = Tweet.jsonToPojo(jsonString);
		Map<String,Tweet> payload = Maps.newConcurrentMap();
		payload.put(tt.getIdAsStr(), tt);
		Map<String,Tweet> enriched = tagger.enrichBatchOfPosts(payload);
		TaggerResponse tr = new TaggerResponse();
		tr.setTaggerResp(enriched);
		String ret =  tr.toString();
		return ret;
	}
}
