package com.igsl.importconfig;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.igsl.ScriptedFieldConversionConstants;

/**
 * JSON structure for CSV import configuration in Jira Cloud
 */
public class ImportConfig {
	
	@JsonProperty("config.version")
	private String configVersion = "2.0";
	
	@JsonProperty("config.project.from.csv")
	private String configProjectFromCsv = "true";
	
	@JsonProperty("config.encoding")
	private String configEncoding = "UTF-8";

	@JsonProperty("config.email.suffix")
	private String configEmailSuffix = "@";
	
	@JsonProperty("config.field.mappings")
	private Map<String, Map<String, String>> configFieldMappings = new HashMap<>();
	
	private void addImportConfig(String csvColumnName, String fieldName) {
		Map<String, String> valueMap = new HashMap<>();
		valueMap.put(ScriptedFieldConversionConstants.JIRA_FIELD, fieldName);
		configFieldMappings.put(csvColumnName, valueMap);
	}
	
	public ImportConfig() {}
	
	public ImportConfig(String scriptedFieldId) {
		configProject = new HashMap<>();
		configProject.put("project.type", null);
		configProject.put("project.key", "");
		configProject.put("project.description", null);
		configProject.put("project.url", null);
		configProject.put("project.name", "");
		configProject.put("project.lead", null);

		addImportConfig("Issue Type", "issuetype");
		addImportConfig("Project Name", "project.name");
		addImportConfig("Project Key", "project.key");
		//addImportConfig("Issue Key", "issuekey");
		addImportConfig("Project Type", "project.type");
		addImportConfig("Summary", "summary");
		addImportConfig(ScriptedFieldConversionConstants.CSV_HEADER_CONVERTED_VALUE, scriptedFieldId);
	}
	
	@JsonProperty("config.value.mappings")
	private Map<String, Object> configValueMappings = new HashMap<>();
	
	@JsonProperty("config.delimiter")
	private String configDelimiter = ",";
	
	@JsonProperty("config.project")
	private Map<String, String> configProject;

	@JsonProperty("config.date.format")
	private String configDateFormat = "dd/MMM/yy h:mm a";

	public String getConfigVersion() {
		return configVersion;
	}

	public void setConfigVersion(String configVersion) {
		this.configVersion = configVersion;
	}

	public String getConfigProjectFromCsv() {
		return configProjectFromCsv;
	}

	public void setConfigProjectFromCsv(String configProjectFromCsv) {
		this.configProjectFromCsv = configProjectFromCsv;
	}

	public String getConfigEncoding() {
		return configEncoding;
	}

	public void setConfigEncoding(String configEncoding) {
		this.configEncoding = configEncoding;
	}

	public String getConfigEmailSuffix() {
		return configEmailSuffix;
	}

	public void setConfigEmailSuffix(String configEmailSuffix) {
		this.configEmailSuffix = configEmailSuffix;
	}

	public Map<String, Map<String, String>> getConfigFieldMappings() {
		return configFieldMappings;
	}

	public void setConfigFieldMappings(Map<String, Map<String, String>> configFieldMappings) {
		this.configFieldMappings = configFieldMappings;
	}

	public Map<String, Object> getConfigValueMappings() {
		return configValueMappings;
	}

	public void setConfigValueMappings(Map<String, Object> configValueMappings) {
		this.configValueMappings = configValueMappings;
	}

	public String getConfigDelimiter() {
		return configDelimiter;
	}

	public void setConfigDelimiter(String configDelimiter) {
		this.configDelimiter = configDelimiter;
	}

	public Map<String, String> getConfigProject() {
		return configProject;
	}

	public void setConfigProject(Map<String, String> configProject) {
		this.configProject = configProject;
	}

	public String getConfigDateFormat() {
		return configDateFormat;
	}

	public void setConfigDateFormat(String configDateFormat) {
		this.configDateFormat = configDateFormat;
	}
}
