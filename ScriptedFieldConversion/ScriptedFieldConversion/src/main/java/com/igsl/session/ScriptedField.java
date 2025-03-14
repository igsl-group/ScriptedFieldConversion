package com.igsl.session;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Scripted field JSON confiugration retrieved from PropertyEntry table
 * Not all properties are mapped
 */
public class ScriptedField {
	public static final String QUERY = 
			"SELECT " + 
			"	t.propertyvalue As Text " + 
			"FROM " + 
			"   propertyentry e " + 
			"	LEFT JOIN propertytext t " + 
			"		ON e.ID = t.ID " + 
			"WHERE " + 
			"	e.PROPERTY_KEY LIKE 'com.onresolve.jira.groovy.groovyrunner:customfields' ";
	
	private static final String CLASS_PREFIX = "com.onresolve.scriptrunner.canned.jira.fields.model.";
	@JsonProperty("@class")
	private String cls;
	private String customFieldId;
	private String name;
	private String desc;
	private String modelTemplate;
	
	@JsonIgnore
	public String getType() {
		if (modelTemplate != null) {
			return modelTemplate;
		}
		if (cls.startsWith(CLASS_PREFIX)) {
			return cls.substring(CLASS_PREFIX.length());
		} else {
			return cls;
		}
	}
	@JsonIgnore
	public String getFullFieldId() {
		return "customfield_" + customFieldId;
	}
	
	public String getCls() {
		return cls;
	}
	/**
	 * This is numeric part only, use getFullFieldId() for customfield_xxxxx
	 */
	public String getCustomFieldId() {
		return customFieldId;
	}
	public void setCustomFieldId(String customFieldId) {
		this.customFieldId = customFieldId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public String getModelTemplate() {
		return modelTemplate;
	}
	public void setModelTemplate(String modelTemplate) {
		this.modelTemplate = modelTemplate;
	}
	public void setCls(String cls) {
		this.cls = cls;
	}
}
