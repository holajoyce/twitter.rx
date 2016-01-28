package com.insightde.taggers.services.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.insightde.ApplicationModeType;
import com.insightde.taggers.dao.Rule;
import com.insightde.taggers.dao.RuleEntryParser;
import com.insightde.taggers.services.Tagger;
import com.insightde.types.GenericPost;
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
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;


public class IodineLuwakTagger  implements Tagger {
	
	private static final Config conf = ConfigFactory.load();
	private static final Logger logger = getLogger(IodineLuwakTagger.class);
	
    public final Analyzer ANALYZER = new StandardAnalyzer();
    public final String FIELD = "text";
	
	private Monitor monitor = null;
	private Monitor monitor_symptoms_only = null;
	private List<MonitorQuery> queries = new ArrayList<>();
	private List<MonitorQuery> queries_symptoms_only = new ArrayList<>();
	
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
		initMonitor();
		registerQueries(dictionaryDir);
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
	public void registerQueries(String dictionaryDir) {
		
		// get the iodine.com definitions from downloaded dictionary files
		IodineJsonParser isp = new IodineJsonParser(dictionaryDir); 

		Map<String, Set<String>>conditions_symptoms = isp.getConditions_symptoms();
		Map<String, Map<String,Set<String>>> conditions_drug_comps = isp.getCondition_drug_companies();
		
		for (Entry<String, Set<String>> entry : conditions_symptoms.entrySet() ) {
			for (String symptom:entry.getValue()){
				String condition = entry.getKey(); 
				Map<String,Set<String>> drug_drugcomp = conditions_drug_comps.get(condition);
				for(String drug_tree:drug_drugcomp.keySet()){
					for (String drug_comp : drug_drugcomp.get(drug_tree)){
						queries.add(new MonitorQuery(drug_comp,symptom));
					}
				}
			}
		}
		
		logger.info("Finished registering queries");
		try {
			monitor.update(queries);
			logger.info("finished updting queries");
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	} // registerQueries
	
	
	public <T> Map<String, T> enrichBatchOfPosts(Map<String, T> posts)  {
		if (posts.isEmpty()) 
			return posts;
		
		List<InputDocument> docs = new ArrayList<>();
		
		for (String key:posts.keySet()){
			docs.add( InputDocument.builder(key).addField( this.FIELD, ((GenericPost) posts.get(key)).getBody(), this.ANALYZER).build());
		}
		DocumentBatch batch = DocumentBatch.of(docs);
		try{
			Matches<HighlightsMatch> matches  = monitor.match(batch, HighlightingMatcher.FACTORY);
			for (DocumentMatches<HighlightsMatch> docMatches : matches) {
	            List<String>condition_tags = Lists.newArrayList();
	            for (HighlightsMatch match : docMatches) {
	            	condition_tags.add(match.getQueryId());
	            }
	            if(!condition_tags.isEmpty()){
	            	GenericPost gp = (GenericPost) posts.get(docMatches.getDocId());
	            	gp.setTags(condition_tags);
	            }
	        } 
		}catch(IOException e){
			logger.error(e.getMessage());
		}
		return posts;
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

//	public Analyzer getANALYZER() {
//		return ANALYZER;
//	}

//	public String getFIELD() {
//		return FIELD;
//	}

//	public  String getDictionaryname() {
//		return dictionaryDir;
//	}

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
	
	
	
	// match batch of docs
}
