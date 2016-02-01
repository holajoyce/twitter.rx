package com.insightde.service.producer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.insightde.utils.IodineJsonParser;
import com.insightde.utils.Nums;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.fluentd.logger.FluentLogger;


@Service
@ConditionalOnProperty(name = "simulate.bidding.produce")
public class BidsProducer {
	
	private static final Config conf = ConfigFactory.load();
	private IodineJsonParser isp = new IodineJsonParser(conf.getString("tagging.iodine.files.directory")); 
	private List<String> all_drug_companies  = null;
	private Map<String, Object> pharmaBids = Maps.newTreeMap();
	 private static FluentLogger LOG = FluentLogger.getLogger("pharma.bids");
	
	// get sorted list of all drug comps
	public BidsProducer(){
		all_drug_companies = Lists.newArrayList();
		all_drug_companies.addAll(isp.getAll_drug_companies());
		Collections.sort(all_drug_companies);
	}
	
	
	// simulate bidding by pharma comps
	// run this every 30 s
	@Scheduled(fixedRate=5000)
	public void produceBidsSimulation(){
		for(String drug_comp : all_drug_companies){
			Double bid =   Nums.round(Nums.getRandomNumberInRange(1, 25)/100.00,2);
			pharmaBids.put(drug_comp,bid.toString());
			LOG.log("price", pharmaBids);
		}
	}
	
	
	
}
