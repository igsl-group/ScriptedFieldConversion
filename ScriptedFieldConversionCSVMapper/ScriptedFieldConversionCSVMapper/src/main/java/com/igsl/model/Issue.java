package com.igsl.model;

import javax.ws.rs.HttpMethod;

import com.igsl.rest.Paged;
import com.igsl.rest.RestUtil;

public class Issue extends JiraObject<Issue> {

	private String id;
	private String key;
	@Override
	public boolean jqlEquals(String value) {
		return 	id.equalsIgnoreCase(value) || 
				key.equalsIgnoreCase(value);
	}
	@Override
	public String getAdditionalDetails() {
		return null;
	}
	@Override
	public String getDisplay() {
		return getKey();
	}
	@Override
	public String getInternalId() {
		return getId();
	}
	@Override
	public String getJQLName() {
		return getKey();
	}
	@Override
	public int compareTo(Issue obj1, boolean exactMatch) {
		if (obj1 != null) {
			return 	STRING_COMPARATOR.compare(getKey(), obj1.getKey());
		}
		return 1;
	}
	@Override
	public void setupRestUtil(RestUtil<Issue> util, boolean cloud, Object... data) {
		util.path("/rest/api/3/search")
			.method(HttpMethod.GET)
			.pagination(new Paged<Issue>(Issue.class).valuesProperty("issues"));
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
}
