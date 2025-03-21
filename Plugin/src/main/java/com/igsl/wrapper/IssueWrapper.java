package com.igsl.wrapper;

import com.atlassian.jira.issue.Issue;

public class IssueWrapper {
	private long id;
	private String key;
	private String summary;	
	public IssueWrapper() {}
	public IssueWrapper(Issue issue) {
		this.id = issue.getId();
		this.key = issue.getKey();
		this.summary = issue.getSummary();
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
}
