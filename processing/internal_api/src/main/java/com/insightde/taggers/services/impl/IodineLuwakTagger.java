package com.insightde.taggers.services.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.insightde.ApplicationModeType;
import com.insightde.taggers.dao.Rule;
import com.insightde.taggers.dao.RuleEntryParser;
import com.insightde.taggers.services.Tagger;
import com.insightde.types.GenericPost;
import com.insightde.types.TT.response.Tweet;
import com.insightde.types.reddit.Reddit;
import com.insightde.types.sources.DataSourceType;
import com.insightde.utils.IodineJsonParser;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import uk.co.flax.luwak.DocumentBatch;
import uk.co.flax.luwak.DocumentMatches;
import uk.co.flax.luwak.InputDocument;
import uk.co.flax.luwak.Matches;
import uk.co.flax.luwak.Monitor;
import uk.co.flax.luwak.MonitorQuery;
import uk.co.flax.luwak.matchers.HighlightingMatcher;
import uk.co.flax.luwak.matchers.HighlightsMatch;
import uk.co.flax.luwak.presearcher.TermFilteredPresearcher;
import uk.co.flax.luwak.queryparsers.LuceneQueryParser;
import static org.apache.log4j.Logger.getLogger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.springframework.beans.BeanUtils;


public class IodineLuwakTagger  implements Tagger {
	
	private static final Config conf = ConfigFactory.load();
	private static final Logger logger = getLogger(IodineLuwakTagger.class);
	
    public final Analyzer ANALYZER = new StandardAnalyzer();
    public final String FIELD = "text";
	
	private Monitor monitor = null;
	private Monitor monitor_symptoms_only = null;
	private List<MonitorQuery> queries = new ArrayList<>();
	IodineJsonParser isp;
	private DataSourceType datasourceType=null;
	
	public IodineLuwakTagger(){
		this(
			ApplicationModeType.findByShortName(conf.getString("application.mode"))
			,conf.getString("tagging.iodine.files.directory")
		);
	}

	public IodineLuwakTagger(
			ApplicationModeType appMode, String dictionaryDir
	){
		isp = new IodineJsonParser(dictionaryDir);
		initMonitor();
		registerQueries();
	}

	
	private void initMonitor(){
		try {
			monitor = new Monitor(new LuceneQueryParser(FIELD, ANALYZER), new TermFilteredPresearcher()) ;
			monitor_symptoms_only = new Monitor(new LuceneQueryParser(FIELD, ANALYZER), new TermFilteredPresearcher()) ;
		} catch (IOException e) {
			logger.error(e.toString());
		}
	}
	
	// usually would be the pdmf dictionary
	public void registerQueries() {
		
		// get the iodine.com definitions from downloaded dictionary files
		
//		for(String symptom: isp.getAll_symptoms()){
//			queries.add(new MonitorQuery(symptom,symptom));
//		}
		
		for(String condition: isp.getCondition_drug_companies().keySet()){
			queries.add(new MonitorQuery(condition,condition));
		}
		

		logger.info("Finished registering queries");
		try {
			monitor.update(queries);
			logger.info("finished updting queries");
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	} // registerQueries
	
	public <T> Map<String, T> enrichPost(Map<String, T> post)  {
		if (post.isEmpty()) 
			return post;
		List<InputDocument> docs = new ArrayList<>();
		Set<String> keys = post.keySet();
		for (String key:keys){
			docs.add( InputDocument.builder(key).addField( this.FIELD, ((GenericPost) post.get(key)).getBody(), this.ANALYZER).build());
		}
		DocumentBatch batch = DocumentBatch.of(docs);
		try{
			Matches<HighlightsMatch> matches  = monitor.match(batch, HighlightingMatcher.FACTORY);
			for (DocumentMatches<HighlightsMatch> docMatches : matches) {
	            Set<String>pharmaTagsSet =  Sets.newHashSet();
	            Set<String>conditionTagsSet = Sets.newHashSet();
	            List<String>symptomTags = Lists.newArrayList();
	            List<String>pharmaTagsList = Lists.newArrayList();
	            Set<String>drugsTags = Sets.newHashSet();
	            
//	            for (HighlightsMatch match : docMatches) {
//	            	String symptom = match.getQueryId();
//	            	symptomTags.add(symptom);
//	            	conditionTagsSet.addAll(isp.getSymptoms_conditions().get(symptom));
//	            	pharmaTagsSet.addAll(isp.getSymptoms_drug_companies().get(symptom));
//	            }
	            
	            for (HighlightsMatch match : docMatches) {
	            	String condition = match.getQueryId();
	            	conditionTagsSet.add(condition);
	            	Map<String,Set<String>>drugs_drugcomps = isp.getCondition_drug_companies().get(condition);
	            	//conditionTagsSet.addAll(isp.getSymptoms_conditions().get(symptom));
	            	drugsTags.addAll(drugs_drugcomps.keySet());
	            	for(String drug : drugs_drugcomps.keySet()){
	            		pharmaTagsSet.addAll(drugs_drugcomps.get(drug));
	            	}
	            }
	            
	            if(!pharmaTagsSet.isEmpty()){
	            	for(String key: post.keySet()){
	            		GenericPost gp = (GenericPost) post.get(key);
	            		
	            		// add pharma tags
	            		pharmaTagsList.addAll(pharmaTagsSet);
	            		Collections.sort(pharmaTagsList);
	            		gp.setPharmatags(pharmaTagsList);
	            		
	            		gp.setConditiontags(conditionTagsSet);
	            		gp.setSymptomtags(symptomTags);
	            		gp.setDrugtags(drugsTags);
	            	}
	            	
	            }
	        } 
		}catch(IOException e){
			logger.error(e.getMessage());
		}
		return post;
	}
	
	
	@Override
	public Monitor getMonitor() {
		return monitor;
	}

	/* (non-Javadoc)
	 * @see com.scc.taggers.services.ITagger#setMonitor(uk.co.flax.luwak.Monitor)
	 */
	@Override
	public void setMonitor(Monitor monitor) {
		this.monitor = monitor;
	}

	public List<MonitorQuery> getQueries() {
		return queries;
	}

	public void setQueries(List<MonitorQuery> queries) {
		this.queries = queries;
	}

	public DataSourceType getDatasourceType() {
		return datasourceType;
	}

	public void setDatasourceType(DataSourceType datasourceType) {
		this.datasourceType = datasourceType;
	}
	
}
