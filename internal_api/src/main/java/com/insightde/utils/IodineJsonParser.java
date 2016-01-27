package com.insightde.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

//import redis.clients.jedis.Jedis;

public class IodineJsonParser {
	
	private Set<String> all_drug_companies = Sets.newHashSet();
	private Map<String,Map<String,Set<String>>> condition_drug_companies = Maps.newHashMap();
	private Map<String, Set<String>> symptoms_conditions = Maps.newHashMap();
	private Map<String,Set<String> > symptoms_drug_companies = Maps.newHashMap();
	
	private Map<String, Set<String>> conditions_symptoms = Maps.newHashMap();
	
	private String filePath = "";
	
	public IodineJsonParser(String filePath){
		this.filePath = filePath;
		readConditionsDir();
	}
	
	private void readConditionsDir(){
//		String fileName =  filePath;
		File dir = new File(filePath);
		File[] fList = dir.listFiles();
		
		for (File file : fList) {
			if(!file.getName().equals(".DS_Store")){
				String content = "";
				try {
					content = new String(Files.readAllBytes(Paths.get(filePath+"/"+file.getName())));
				} catch (IOException e) {
					e.printStackTrace();
				}
				readConditionFromJson(content);
			}
		}
		System.out.println(condition_drug_companies);
	}
	
	// symptoms -> condition
	//  conditions -> drugs -> drug companies
	private void readConditionFromJson(String json){
		
		ReadContext ctx = JsonPath.parse(json);
		
		Map<String,Set<String>> drug_names_companies = Maps.newHashMap();
		try{
			
			String condition = ctx.read("$.condition.name");
			condition_drug_companies.put(condition,drug_names_companies);
			Set<String> symptoms = new HashSet<String>(ctx.read("$.condition.symptoms"));
			conditions_symptoms.put(condition, symptoms);
			insertIntoConditionsSymptoms(symptoms, condition);
//			#insertIntoSymptomsDrugCompanies(conditions,drug_companies);
			
			List<String> drugs_names = ctx.read("$.drugs[*].name");		
			for(Integer i=0;i<drugs_names.size();i++){
				Set<String> drug_companies = new HashSet<String>(ctx.read("$.drugs["+i.toString()+"].images[*].labeler"));
				if(drug_companies.size()>0){
					drug_names_companies.put(drugs_names.get(i), drug_companies);
					all_drug_companies.addAll(drug_companies);
					insertIntoSymptomsDrugComps(symptoms,drug_companies);
				}
			}
			
		}catch(Exception e){
		}
	}
	
	private void insertIntoSymptomsDrugComps(Set<String>symptoms,Set<String>drug_comps){
		for(String symptom:symptoms){
			if(!symptoms_drug_companies.containsKey(symptom)){
				symptoms_drug_companies.put(symptom, drug_comps);
			}else{
				Set<String> old_drug_comps = symptoms_drug_companies.get(symptom);
				for(String drug_comp: drug_comps){
					old_drug_comps.add(drug_comp);
				}
				symptoms_drug_companies.put(symptom,old_drug_comps );
			}
		}
	}

	private void insertIntoConditionsSymptoms(Set<String>symptoms, String condition){
		for (String symptom :symptoms){
			if (!symptoms_conditions.containsKey(symptom)){
				Set<String> conditionSet= new HashSet<String>(Arrays.asList(condition));
				symptoms_conditions.put(symptom, conditionSet);
			}else{
				Set<String>conditionSet = symptoms_conditions.get(symptom);
				conditionSet.add(condition);
			}
			
		}
	}
	

	public Map<String, Map<String, Set<String>>> getCondition_drug_companies() {
		return condition_drug_companies;
	}

	public Map<String, Set<String>> getSymptoms_conditions() {
		return symptoms_conditions;
	}

	public Map<String, Set<String>> getConditions_symptoms() {
		return conditions_symptoms;
	}

	public Map<String, Set<String>> getSymptoms_drug_companies() {
		return symptoms_drug_companies;
	}

	public void setSymptoms_drug_companies(Map<String, Set<String>> symptoms_drug_companies) {
		this.symptoms_drug_companies = symptoms_drug_companies;
	}
	
}
