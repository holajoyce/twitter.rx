package com.insightde.taggers.services.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.insightde.ApplicationModeType;
import com.insightde.taggers.dao.Rule;
import com.insightde.taggers.dao.RuleEntryParser;
import com.insightde.taggers.services.Tagger;
import com.insightde.types.GenericPost;
import com.insightde.types.sources.DataSourceType;
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
import java.util.stream.Collectors;
import java.util.ArrayList;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;


public class ExperimentalLuwakTagger  implements Tagger {
	
	private static final Config conf = ConfigFactory.load();
	private static final Logger logger = getLogger(ExperimentalLuwakTagger.class);
	
    public final Analyzer ANALYZER = new StandardAnalyzer();
    public final String FIELD = "text";
	
    private ApplicationModeType applicationMode = ApplicationModeType.findByShortName(conf.getString("application.mode"));
    
	private Map<String,String> lineToMainCategoryMapping = Maps.newHashMap();
	private Map<String,String> lineToSubCategoryMapping = Maps.newHashMap();
	private Map<String,String> subCategoryToMainCategoryMapping = Maps.newHashMap();
	private Map<String,String> subCategoryToLineMapping = Maps.newHashMap();
	private Map<String,String> mainCategoryToLineMapping = Maps.newHashMap();
	private Map<String,String> mainCategoryToSubCategoryMapping = Maps.newHashMap();
	
	List<Rule> rules = Lists.newArrayList();
	
	private  String dictionaryName; // = conf.getString("tagging.dictionary.names");
	private  String taggingFilesDirectory; //= conf.getString("tagging."+dictionaryName+".files.directory."+applicationMode.getLongname());
	
	private Monitor monitor = null;
	private Monitor rootMonitor = null;
	private List<MonitorQuery> rootQueries = new ArrayList<>();
	private List<MonitorQuery> queries = new ArrayList<>();
	
	private DataSourceType datasourceType=null;
	
	

	public ExperimentalLuwakTagger(
			ApplicationModeType appMode, String dictionaryName
			, String taggingFilesDir
	){
		applicationMode = appMode;
		this.dictionaryName = dictionaryName;
		this.taggingFilesDirectory = taggingFilesDir;
		initMonitor();
		registerQueries(dictionaryName);
		registerRules(dictionaryName);
	}

	public ExperimentalLuwakTagger(){
		dictionaryName = conf.getString("tagging.dictionary.names");
		taggingFilesDirectory = conf.getString("tagging."+dictionaryName+".files.directory."+applicationMode.getLongname());
		initMonitor();
		registerQueries(dictionaryName);
		registerRules(dictionaryName);
	}
	
	private void initMonitor(){
		try {
			monitor = new Monitor(new LuceneQueryParser(FIELD, ANALYZER), new TermFilteredPresearcher()) ;
			rootMonitor = new Monitor(new LuceneQueryParser(FIELD, ANALYZER), new TermFilteredPresearcher()) ;
		
		} catch (IOException e) {
			logger.error(e.toString());
		}
	}
	private void registerRules(String dictionaryName) /*throws FileNotFoundException, IOException */{
		File file;
		if(applicationMode==ApplicationModeType.PROD){
			ClassLoader classLoader = getClass().getClassLoader();
			String fileName = taggingFilesDirectory + dictionaryName+"_rules.csv";
			file = new File(classLoader.getResource(fileName).getFile());
		}
		else{
			file = new File(taggingFilesDirectory + dictionaryName+"_rules.csv");
			logger.info(taggingFilesDirectory + dictionaryName);
		}
		
		try{
			FileReader fileReader = new FileReader(file);
			rules =  new RuleEntryParser(dictionaryName).readCsvFile(fileReader);
			for(Rule rule :rules){
				rootQueries.add(new MonitorQuery(rule.getMainTheme(), rule.getRule()));
			}
			rootMonitor.update(rootQueries);
			logger.info(rules);
			
			fileReader.close();
		}catch(Exception e){
			logger.error(e.getStackTrace());
		}
	}
	
	private void setDictionaries(String mainTheme,String subTheme, String line){
		lineToMainCategoryMapping.put(line, mainTheme);
		lineToSubCategoryMapping.put(line, subTheme);
		subCategoryToLineMapping.put(subTheme,line);
		mainCategoryToLineMapping.put(mainTheme,line);
		mainCategoryToSubCategoryMapping.put(mainTheme, subTheme);
	}
	
	// read from directory of queries (dir name is dictionary)
	// usually would be the pdmf dictionary
	public void registerQueries(String dictionaryName) {
		logger.info(">>>> registering queries");
		
		// open the tagging listing & start ingesting
		File dir;
		if(applicationMode==ApplicationModeType.PROD){
			ClassLoader classLoader = getClass().getClassLoader();
			String fileName = taggingFilesDirectory + "/" + dictionaryName;
			dir = new File(classLoader.getResource(fileName).getFile());
		}
		else{
			dir = new File(taggingFilesDirectory + dictionaryName);
			logger.info(taggingFilesDirectory + dictionaryName);
		}
		File[] fList = dir.listFiles();
		for (File subdir : fList) {
			if (subdir.isDirectory()) {
				logger.info("(main theme) subdir: " + subdir.getName());
				File[] fListSub = subdir.listFiles();
				for (File file : fListSub) {
					try{
						if (!file.getName().equals(".DS_Store") && file.isFile() && FilenameUtils.getExtension(file.getCanonicalPath()).equals("csv")) {
							logger.info("(subtheme) found file with name: " + file.getName());
							subCategoryToMainCategoryMapping.put(file.getName().replace(".csv", ""), subdir.getName());
							BufferedReader reader = new BufferedReader(new FileReader(file));
							try {
								String line = reader.readLine().toLowerCase().trim();
								logger.info("(query) example query line: " + line);
								queries.add(new MonitorQuery(file.getName().replace(".csv", ""), line));
								setDictionaries(subdir.getName(), file.getName().replace(".csv", ""), line);
								while (line != null) {
									line = reader.readLine().toLowerCase().trim();
									queries.add(new MonitorQuery(file.getName().replace(".csv", ""), line));
									logger.info("(query) example query line: " + line);
									setDictionaries(subdir.getName(), file.getName().replace(".csv", ""), line);
								}
							reader.close();
							} catch (Exception e) {
								// sometimes line break errors
							}
						}
					}catch(FileNotFoundException e){
						logger.error(e.getStackTrace());
					}catch(IOException e){
						logger.error(e.getStackTrace());
					}
				}
			}
		} // end for
		logger.info("done");
		try {
			monitor.update(queries);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	} // registerQueries
	
	public <T> Map<String, T> enrichBatchOfPosts(Map<String, T> posts)  {
		if (posts.isEmpty()) 
			return posts;
		
		List<InputDocument> docs = new ArrayList<>();
		List<InputDocument> rootDocs = new ArrayList<>();
		
		// pdmf tag at leaf level
		for (String key:posts.keySet()){
			docs.add( InputDocument.builder(key).addField( getFIELD(), ((GenericPost) posts.get(key)).getText(), getANALYZER()).build());
		}
		DocumentBatch batch = DocumentBatch.of(docs);
		try{
			Matches<HighlightsMatch> matches  = monitor.match(batch, HighlightingMatcher.FACTORY);
			for (DocumentMatches<HighlightsMatch> docMatches : matches) {
	            List<String>pdmf_tags = Lists.newArrayList();
	            for (HighlightsMatch match : docMatches) {
	            	pdmf_tags.add(match.getQueryId());
	            }
	            if(!pdmf_tags.isEmpty()){
	            	GenericPost gp = (GenericPost) posts.get(docMatches.getDocId());
	            	gp.setTags(pdmf_tags);
	            }
	        } 
		}catch(IOException e){
			logger.error(e.getMessage());
		}
		
		// pdmf root tags
		for (String key:posts.keySet()){
			rootDocs.add( InputDocument.builder(key).addField( getFIELD(), ((GenericPost) posts.get(key)).getTags().stream().map(i->i.toString()).collect(Collectors.joining(", ")), getANALYZER()).build());
		}
		DocumentBatch rootBatch = DocumentBatch.of(rootDocs);
		try{
			Matches<HighlightsMatch> rootMatches  = rootMonitor.match(rootBatch, HighlightingMatcher.FACTORY);
			for (DocumentMatches<HighlightsMatch> docMatches : rootMatches) {
				List<String>root_pdmf_tags = Lists.newArrayList();
				for (HighlightsMatch match : docMatches) {
					root_pdmf_tags.add(match.getQueryId());
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

	public Analyzer getANALYZER() {
		return ANALYZER;
	}

	public String getFIELD() {
		return FIELD;
	}

	public  String getDictionaryname() {
		return dictionaryName;
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


	// match single doc
	public Monitor getRootMonitor() {
		return rootMonitor;
	}

	public void setRootMonitor(Monitor rootMonitor) {
		this.rootMonitor = rootMonitor;
	}
	public List<MonitorQuery> getRootQueries() {
		return rootQueries;
	}

	public void setRootQueries(List<MonitorQuery> rootQueries) {
		this.rootQueries = rootQueries;
	}
	
	// match batch of docs

}
