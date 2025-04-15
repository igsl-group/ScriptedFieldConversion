package com.igsl.session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.project.Project;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igsl.Log;
import com.igsl.action.ActionHandler;
import com.igsl.action.DataAction;
import com.igsl.action.FieldAction;
import com.igsl.action.ScreenAction;
import com.igsl.dataconversion.DataConversionType;

/**
 * Stores all data in a single object
 */
public class SessionData {
	private static final ObjectMapper OM = new ObjectMapper();
	private static final Logger LOGGER = Logger.getLogger(SessionData.class);
	
	private static final String SESSION_DATA_NAME = "ScriptedFieldConversionData";
	private static final String PARAM_FILTER = "filter";
	private Map<String, DataRow> dataRows = new TreeMap<>();
	private String filter;
	// Full list of custom fields, key is display name
	@JsonIgnore
	private Map<String, List<CustomField>> customFields = null;
	@JsonIgnore
	private Map<String, List<ProjectInfo>> usedInProjects = new HashMap<>();
	@JsonIgnore
	private Map<String, List<ScreenInfo>> usedInScreens = new HashMap<>();	
	@JsonIgnore
	private String sql = "";
	@JsonIgnore
	private String sqlResult = "";
	@JsonIgnore
	private List<Project> projects = new ArrayList<>();
	@JsonIgnore
	private ByteArrayOutputStream zipBytes = null;
	@JsonIgnore 
	private ZipOutputStream zipOutputStream = null;
	@JsonIgnore 
	private boolean autoRefresh;
	
	public void setupExportData() {
		zipBytes = new ByteArrayOutputStream();
		zipOutputStream = new ZipOutputStream(zipBytes);
	}
	public void clearExportData() {
		if (zipOutputStream != null) {
			try {
				zipOutputStream.close();
			} catch (IOException e) {
				// Ignore
			}
			zipOutputStream = null;
		}
		if (zipBytes != null) {
			try {
				zipBytes.close();
			} catch (IOException e) {
				// Ignore
			}
			zipBytes = null;
		}
	}
	
	public void save(HttpSession session) {
		session.setAttribute(SESSION_DATA_NAME, this);
	}
	
	public static SessionData load(HttpSession session) {
		SessionData result = (SessionData) session.getAttribute(SESSION_DATA_NAME);
		if (result == null) {
			result = new SessionData();
		}
		return result;
	}
	
	public void parseActions(HttpServletRequest req) {
		Map<String, String[]> parameters = req.getParameterMap();
		filter = req.getParameter(PARAM_FILTER);
		try {
			Log.debug(LOGGER, "Parameter Map: " + OM.writeValueAsString(parameters));
		} catch (Exception ex) {
			Log.error(LOGGER, "Parameter Map", ex);
		}
		Map<String, FieldAction> fieldActions = 
				ActionHandler.parseAction(FieldAction.class, parameters, FieldAction.NONE);
		Map<String, DataAction> dataActions = 
				ActionHandler.parseAction(DataAction.class, parameters, DataAction.NONE);
		Map<String, ScreenAction> screenActions = 
				ActionHandler.parseAction(ScreenAction.class, parameters, ScreenAction.NONE);
		Map<String, String> replacementFieldIds = ActionHandler.parseParameter("replacementField", parameters);
		Map<String, String> projects = ActionHandler.parseParameter("projects", parameters);
		Map<String, DataConversionType> dataConversions = 
				ActionHandler.parseAction(DataConversionType.class, parameters, DataConversionType.NONE);
		for (Map.Entry<String, DataRow> entry : dataRows.entrySet()) {
			entry.getValue().setFieldAction(fieldActions.getOrDefault(entry.getKey(), FieldAction.NONE));
			entry.getValue().setScreenAction(screenActions.getOrDefault(entry.getKey(), ScreenAction.NONE));
			entry.getValue().setDataAction(dataActions.getOrDefault(entry.getKey(), DataAction.NONE));
			// Replacement field ID is a string, so the N/A entry value is a space. So trim it here.
			entry.getValue().setReplacementFieldId(
					replacementFieldIds.containsKey(entry.getKey()) ?
					replacementFieldIds.get(entry.getKey()).trim() :
					null);
			entry.getValue().setProjects(
					projects.containsKey(entry.getKey()) ? 
					projects.get(entry.getKey()).trim() : 
					null
					);
			entry.getValue().setDataConversionType(
					dataConversions.getOrDefault(entry.getKey(), DataConversionType.NONE));
		}
	}
	
	public Map<String, DataRow> getDataRows() {
		return dataRows;
	}
	public void setDataRows(Map<String, DataRow> dataRows) {
		this.dataRows = dataRows;
	}
	public Map<String, List<CustomField>> getCustomFields() {
		return customFields;
	}
	public void setCustomFields(Map<String, List<CustomField>> customFields) {
		this.customFields = customFields;
	}
	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {
		this.sql = sql;
	}
	public String getSqlResult() {
		return sqlResult;
	}
	public void setSqlResult(String sqlResult) {
		this.sqlResult = sqlResult;
	}
	public Map<String, List<ScreenInfo>> getUsedInScreens() {
		return usedInScreens;
	}
	public void setUsedInScreens(Map<String, List<ScreenInfo>> usedInScreens) {
		this.usedInScreens = usedInScreens;
	}
	public List<Project> getProjects() {
		return projects;
	}
	public void setProjects(List<Project> projects) {
		this.projects = projects;
	}
	public ByteArrayOutputStream getZipBytes() {
		return zipBytes;
	}
	public ZipOutputStream getZipOutputStream() {
		return zipOutputStream;
	}
	public boolean isAutoRefresh() {
		return autoRefresh;
	}
	public void setAutoRefresh(boolean autoRefresh) {
		this.autoRefresh = autoRefresh;
	}
	public Map<String, List<ProjectInfo>> getUsedInProjects() {
		return usedInProjects;
	}
	public void setUsedInProjects(Map<String, List<ProjectInfo>> usedInProjects) {
		this.usedInProjects = usedInProjects;
	}
	public String getFilter() {
		return filter;
	}
	public void setFilter(String filter) {
		this.filter = filter;
	}
}
