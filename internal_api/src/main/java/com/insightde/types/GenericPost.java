package com.insightde.types;

import java.util.List;

public interface GenericPost {
	
	public List<String> getPdmf_tags();
	public void setPdmf_tags(List<String> pdmf_tags);
	
	public List<String> getMain_pdmf_tags();
	public void setMain_pdmf_tags(List<String> main_pdmf_tags);
	
	public String getRelevantText();
}
