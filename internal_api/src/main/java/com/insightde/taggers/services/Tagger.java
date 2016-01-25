package com.insightde.taggers.services;

import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;

import com.insightde.types.sources.DataSourceType;

import uk.co.flax.luwak.Monitor;
import uk.co.flax.luwak.MonitorQuery;

public interface Tagger {

	Monitor getMonitor();
	void setMonitor(Monitor monitor);
	public Monitor getRootMonitor() ;
	public void setRootMonitor(Monitor rootMonitor) ;
	public Analyzer getANALYZER() ;
	public String getFIELD() ;
	public  String getDictionaryname() ;
	public List<MonitorQuery> getQueries() ;
	public void setQueries(List<MonitorQuery> queries) ;
	public <T> Map<String, T> enrichBatchOfPosts(Map<String, T> posts);
//	public List<SinkType> getOutputTypes() ;
//	public void setOutputTypes(List<SinkType> outputTypes);
	public DataSourceType getDatasourceType() ;
	public void setDatasourceType(DataSourceType datasourceType);
//	public IOutputDao getOutputDao() ;
//	public void setOutputDao(IOutputDao outputDao);
}