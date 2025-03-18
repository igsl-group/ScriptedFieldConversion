package com.igsl.dataconversion;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.jira.issue.Issue;


/**
 * Interface for data conversion
 * 
 * JCMA doesn't do jack with HTML in issue fields: 
 * https://confluence.atlassian.com/migrationkb/custom-fields-with-html-or-javascript-shows-as-plain-text-after-migration-1108478848.html
 * 
 * So we must convert the data on our own and import the converted values in Cloud.
 * 
 * Two types are handled:
 * 1. Fix Tickets - Scripted field is multi-issue picker. Need to convert to linked issues.
 * 2. Quote Response/s - Scripted field is HTML table. Need to convert to markdown.
 */
public abstract class DataConversion {
	
	/**
	 * Object type. 
	 * If global, the data is cached in globalMappings.
	 * If not global, the data is prepared according to getMappingConstraints().
	 */
	public enum ObjectType {
		ISSUE(false),
		ISSUE_TYPE(true),
		CUSTOM_FIELD(true),
		PROJECT(true);
		private boolean global;	// If global, the data is cached for all DataConversion to use
		private ObjectType(boolean global) {
			this.global = global;
		}
		public boolean isGlobal() {
			return global;
		}
	}
	
	/**
	 * All data from Jira Cloud required by any of the DataConversion subclasses
	 * Second level key is name, value is object id.
	 */
	protected static Map<ObjectType, Map<String, String>> globalMappings;
	public static void setGlobalMappings(Map<ObjectType, Map<String, String>> globalMappings) {
		DataConversion.globalMappings = globalMappings;
	}
	public static Map<ObjectType, Map<String, String>> getGlobalMappings() {
		return globalMappings;
	}
	
	/**
	 * Mapping data specific to one DataConversion subclass.
	 */
	protected Map<ObjectType, Map<String, String>> localMappings;
	public void setLocalMappings(Map<ObjectType, Map<String, String>> localMappings) {
		this.localMappings = localMappings;
	}
	public Map<ObjectType, Map<String, String>> getLocalMappings() {
		return localMappings;
	}

	/**
	 * Convert sourceValue into new format. 
	 * Issue object provided as additional information.
	 */
	public abstract Object convert(
			Issue issue, 
			Object sourceValue) throws Exception;

	/**
	 * Return parameters to use in REST API for specific ObjectType.
	 */
	public abstract Map<ObjectType, Map<String, Object>> getMappingConstraints();
	
	/**
	 * Remap object ids in value provided by convert().
	 */
	public abstract String remap(String sourceValue) throws Exception;
}