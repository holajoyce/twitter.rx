package com.insightde.types;

import java.util.List;

public interface GenericPost {
	
	public List<String> getPharmatags();
	public void setPharmatags(List<String> pharmatags);
	
	public List<String> getSymptomtags();
	public void setSymptomtags(List<String> symptomtags);
	
	public String getBody();
}
