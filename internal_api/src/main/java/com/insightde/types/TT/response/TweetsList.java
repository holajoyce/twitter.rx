package com.insightde.types.TT.response;

import java.util.List;

import com.google.common.collect.Lists;

public class TweetsList{
	
    private List<Tweet> tweets = Lists.newArrayList();

	public List<Tweet> getTweets() {
		return tweets;
	}

	public void setTweets(List<Tweet> tweets) {
		this.tweets = tweets;
	}
    
}