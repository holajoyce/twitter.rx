package com.insightde.service.producer;

import static org.apache.log4j.Logger.getLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.insightde.service.restful.TaggerController;
import com.insightde.utils.IodineJsonParser;
import com.insightde.utils.Nums;
import com.insightde.utils.ValueComparableMap;
import com.insightde.utils.clients.Rest;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import sun.rmi.runtime.Log;

import org.fluentd.logger.FluentLogger;
import com.insightde.utils.ValueComparableMap;

@Service
@ConditionalOnProperty(name = "simulate.bidding.produce")
public class BidsProducer {

	final static org.apache.log4j.Logger logger = getLogger(BidsProducer.class);
	private static final Config conf = ConfigFactory.load();
	private IodineJsonParser isp = new IodineJsonParser(conf.getString("tagging.iodine.files.directory"));
	private Map<String, Object> pharmaBids = Maps.newTreeMap();
	private static FluentLogger LOG = FluentLogger.getLogger("pharma.bids");
	private static final String ES_HOST = conf.getString("elasticsearch.host");

	// get sorted list of all drug comps
	public BidsProducer() {

	}

	// simulate bidding by pharma comps
	// run this every 0.5 s
	@Scheduled(fixedRate = 1000)
	public void produceBidsSimulation() {
		TreeMap<String, Double> bidItems = new ValueComparableMap<String, Double>(Ordering.natural());
		
		for (String drug_comp : IodineJsonParser.getAll_drug_companies_list()) {
			bidItems.put(drug_comp,  Nums.round(Nums.getRandomNumberInRange(1, 90) / 100.00, 2));
		}
		
		Iterator<Entry<String,Double>> iterator = bidItems.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String,Double> entry = (Map.Entry<String,Double>) iterator.next();
			Double newPrice = Nums.round( (1-entry.getValue()),2);
			logger.info("Key : " + entry.getKey() + " Value :" +newPrice);
			
			Map<String,Object> bidItem=Maps.newTreeMap();
			bidItem.put("pharmatags", entry.getKey() );
			bidItem.put("price",newPrice);
			LOG.log("prices", bidItem);
		}
	}

	//deprecated
//	@Scheduled(fixedRate = 259200)
//	public void updateDrugCompaniesOrdering() {
//		List<String> difn = new ArrayList<String>(IodineJsonParser.getAll_drug_companies_list());
//		String response = Rest.post("http://" + ES_HOST + ":9200/_search", readElasticSearchRequestFile());
//		ReadContext ctx = JsonPath.parse(response);
//		List<String> top_drug_companies_lst = new ArrayList<String>(ctx.read("$.aggregations.2.buckets[*].key"));
//		difn.removeAll(top_drug_companies_lst);
//		List<String> newOrderings = new ArrayList<String>(top_drug_companies_lst);
//		newOrderings.addAll(difn);
//		IodineJsonParser.setAll_drug_companies_list(newOrderings);
//		logger.info(newOrderings.size());
//	}

	private String readElasticSearchRequestFile() {
		String content = "";
		try {
			content = new String(
					Files.readAllBytes(Paths.get("src/main/resources/request_top_bidder_from_elasticsearch.json")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

}
