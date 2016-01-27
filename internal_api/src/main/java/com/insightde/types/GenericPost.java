package com.insightde.types;

import java.util.List;

public interface GenericPost {
	
	public List<String> getTags();
	public void setTags(List<String> tags);
	
//	public String getRelevantText();
	public String getText();
}
