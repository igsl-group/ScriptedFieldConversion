package com.igsl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.database.ConnectionFunction;
import com.atlassian.jira.database.DatabaseAccessor;
import com.atlassian.jira.database.DatabaseConnection;
import com.atlassian.jira.exception.RemoveException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.context.GlobalIssueContext;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.CustomFieldSearcher;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.config.manager.FieldConfigSchemeManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenTab;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobRunnerKey;
import com.atlassian.scheduler.config.RunMode;
import com.atlassian.scheduler.config.Schedule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.igsl.action.DataAction;
import com.igsl.action.FieldAction;
import com.igsl.action.ScreenAction;
import com.igsl.dataconversion.DataConversionType;
import com.igsl.job.CopyDataJob;
import com.igsl.job.ExportDataJob;
import com.igsl.job.Job;
import com.igsl.job.JobEntity;
import com.igsl.session.ActionLog;
import com.igsl.session.DataRow;
import com.igsl.session.ScreenInfo;
import com.igsl.session.ScriptedField;
import com.igsl.session.ScriptedFieldType;
import com.igsl.session.SessionData;

import webwork.action.ServletActionContext;
import webwork.multipart.MultiPartRequestWrapper;

@Named
public class ScriptedFieldConversion extends JiraWebActionSupport {

	private static final SchedulerService SCHEDULER_SERVICE = 
			ComponentAccessor.getComponent(SchedulerService.class);
	private static final DatabaseAccessor DATABASE_ACCESSOR = 
			ComponentAccessor.getComponent(DatabaseAccessor.class);
	private static final CustomFieldManager CUSTOM_FIELD_MANAGER = 
			ComponentAccessor.getCustomFieldManager();
	private static final FieldConfigSchemeManager FIELD_CONFIG_SCHEME_MANAGER = 
			ComponentAccessor.getFieldConfigSchemeManager();
	private static final FieldScreenManager FIELD_SCREEN_MANAGER = 
			ComponentAccessor.getFieldScreenManager();
	private static final JqlQueryParser JQL_QUERY_PARSER = 
			ComponentAccessor.getComponent(JqlQueryParser.class);
	private static final SearchService SEARCH_SERVICE = 
			ComponentAccessor.getComponent(SearchService.class);
	private static final ProjectManager PROJECT_MANAGER = 
			ComponentAccessor.getProjectManager();
	
	private static final ObjectMapper OM = new ObjectMapper()
			// Indent
			.enable(SerializationFeature.INDENT_OUTPUT)
			// Allow unknow properties
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			// Sort maps
			.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
	
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(ScriptedFieldConversion.class);
	
	private SessionData sessionData;

	@SuppressWarnings("unused")
	private ActiveObjects ao;
	
	@Inject
	protected ScriptedFieldConversion(@ComponentImport ActiveObjects ao) {
		super();
		Log.debug(LOGGER, "PluginDEBUG ao: " + ao);
        this.ao = ao;
        Log.debug(LOGGER, "PluginDEBUG ao initialized: " + ao.moduleMetaData().isInitialized());
        Job.setActiveObejcts(ao);
		Log.debug(LOGGER, "PluginDEBUG ScriptFieldConversion constructed");
	}
	
	/**
	 * Expose ObjectMapper for velocity template
	 */
	public String convertToJson(Object o) {
		try {
			return OM.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			return "JSON Error: " + e.getMessage();
		}
	}

	/**
	 * Exposes data for velocity template
	 */
	public boolean isAutoRefresh() {
		return sessionData.isAutoRefresh();
	}
	public DataAction[] getAllDataActions() {
		return DataAction.values();
	}
	public FieldAction[] getAllFieldActions() {
		return FieldAction.values();
	}
	public ScreenAction[] getAllScreenActions() {
		return ScreenAction.values();
	}
	public Collection<DataRow> getDataRows() {
		return sessionData.getDataRows().values();
	}
	public Map<String, List<ScreenInfo>> getScreenInfoMap() {
		return sessionData.getUsedInScreens();
	}
	public String getSqlResult() {
		return sessionData.getSqlResult();
	}
	public String getSql() {
		return sessionData.getSql();
	}
	public Map<String, List<CustomField>> getCustomFields() {
		return sessionData.getCustomFields();
	}
	public List<Project> getProjects() {
		return sessionData.getProjects();
	}
	public boolean hasZip() {
		return (sessionData.getZipOutputStream() != null);
	}
	public DataConversionType[] getDataConversionTypes() {
		return DataConversionType.values();
	}
	public List<JobEntity> getAllJobEntity() {
		return Job.loadAllJobEntity();
	}
	
	private void fetchProjects() {
		sessionData.setProjects(PROJECT_MANAGER.getProjects());
	}
	
	private void fetchUsedInScreens() {
		Set<String> fieldIdSet = new HashSet<>();
		// Collect field ids
		for (List<CustomField> cfList : sessionData.getCustomFields().values()) {
			for (CustomField cf : cfList) {
				fieldIdSet.add(cf.getId());
			}
		}
		if (fieldIdSet.size() != 0) {
			// Fetch screen usage from database
			String query = ScreenInfo.getQuerySQL(fieldIdSet.size());
			Map<String, List<ScreenInfo>> map = DATABASE_ACCESSOR.executeQuery(
				new ConnectionFunction<Map<String, List<ScreenInfo>>>() {
					@Override
					public Map<String, List<ScreenInfo>> run(DatabaseConnection dbConn) {
						Map<String, List<ScreenInfo>> result = new HashMap<>(); 
						Connection conn = dbConn.getJdbcConnection();
						try (PreparedStatement ps = conn.prepareStatement(query.toString())) {
							int idx = 1;
							for (String id : fieldIdSet) {
								ps.setString(idx, id);
								idx++;
							}
							ResultSet rs = ps.executeQuery();
							while (rs.next()) {
								ScreenInfo info = new ScreenInfo(rs);
								if (!result.containsKey(info.getFieldId())) {
									result.put(info.getFieldId(), new ArrayList<>());
								}
								result.get(info.getFieldId()).add(info);
							}
						} catch (SQLException sqlex) {
							Log.error(LOGGER, "SQLException", sqlex);
						}
						return result;
					}
				});
			sessionData.setUsedInScreens(map);
		}
	}
	
	private void fetchScriptedFields() {
		// Load information about Scripted Fields from database
		ObjectReader reader = OM.readerFor(ScriptedField.class);
		List<ScriptedField> fieldList = DATABASE_ACCESSOR.executeQuery(
				new ConnectionFunction<List<ScriptedField>>() {
					@Override
					public List<ScriptedField> run(DatabaseConnection dbConn) {
						List<ScriptedField> result = new ArrayList<>();
						Connection conn = dbConn.getJdbcConnection();
						try (	PreparedStatement ps = conn.prepareStatement(ScriptedField.QUERY);
								ResultSet rs = ps.executeQuery()) {
							while (rs.next()) {
								try {
									String data = rs.getString(1);
									if (data != null && !data.isEmpty()) {
										MappingIterator<ScriptedField> it = reader.readValues(data);
										while (it.hasNext()) {
											ScriptedField conf = it.next();
											result.add(conf);
										}
									}
								} catch (IOException ioex) {
									Log.error(LOGGER, "JSON Error", ioex);
								}
							}
						} catch (SQLException sqlex) {
							Log.error(LOGGER, "SQLException", sqlex);
						}
						return result;
					}
				});
		for (ScriptedField field : fieldList) {
			if (sessionData.getDataRows().containsKey(field.getFullFieldId())) {
				// Update existing data row
				sessionData.getDataRows().get(field.getFullFieldId()).setScriptedField(field);
			} else {
				// Add new data row
				DataRow row = new DataRow();
				row.setScriptedField(field);
				sessionData.getDataRows().put(field.getFullFieldId(), row);
			}
		}
	}
	
	private void fetchCustomFields() {
		Map<String, List<CustomField>> map = new HashMap<>();
		for (CustomField cf : CUSTOM_FIELD_MANAGER.getCustomFieldObjects()) {
			if (!map.containsKey(cf.getName())) {
				map.put(cf.getName(), new ArrayList<>());
			}
			map.get(cf.getName()).add(cf);
		}
		sessionData.setCustomFields(map);
	}

	private void initSession() {
		sessionData = new SessionData();
		fetchProjects();
		fetchCustomFields();
		fetchScriptedFields();
		fetchUsedInScreens();
		saveSession();
	}
	
	private void saveSession() {
		sessionData.save(getHttpRequest().getSession());
	}
	private void loadSession() {
		sessionData = SessionData.load(getHttpRequest().getSession());
		// Keep data up-to-date
		fetchProjects();
		fetchCustomFields();
		fetchScriptedFields();	// This will not purge missing scripted fields
		fetchUsedInScreens();
		// Update data
	}

	public String doInit() throws Exception {
		initSession();
		return JiraWebActionSupport.INPUT;
	}
	
	private CustomFieldType<?, ?> getCustomFieldType(CustomFieldTypeHelper type) {
		if (type != null) {
			return CUSTOM_FIELD_MANAGER.getCustomFieldType(type.getTypeKey());
		}
		return null;
	}

	private CustomFieldSearcher getCustomFieldSearcher(CustomFieldTypeHelper type) {
		if (type != null) {
			return CUSTOM_FIELD_MANAGER.getCustomFieldSearcher(type.getSearcherKey());
		}
		return null;
	}

	private void deleteReplacementField(DataRow row) {
		String msg = "";
		CustomField replacementField = CUSTOM_FIELD_MANAGER.getCustomFieldObject(row.getReplacementFieldId());
		if (replacementField != null) {
			try {
				CUSTOM_FIELD_MANAGER.removeCustomField(replacementField);
				msg = "Replacement field " + row.getReplacementFieldId() + " removed";
				row.getActionLog().getFieldAction().add(msg);
				row.setReplacementFieldId(null);
			} catch (RemoveException rex) {
				msg = "Unable to remove replacement field " + row.getReplacementFieldId() + ": " + rex.getMessage();
				row.getActionLog().getFieldAction().add(msg);
			}
		} else {
			msg = "Replacement field " + row.getReplacementFieldId() + " not found";
			row.getActionLog().getFieldAction().add(msg);
		}
	}
	
	private void createReplacementField(DataRow row, CustomField originalField) {
		// Create replacement field based on template/class
		ScriptedField scriptedField = row.getScriptedField();
		ScriptedFieldType scriptedFieldType = ScriptedFieldType.parse(scriptedField.getModelTemplate());
		// Determine custom field type and searcher
		CustomFieldType<?, ?> cfType = getCustomFieldType(scriptedFieldType.getHelper());
		CustomFieldSearcher cfSearcher = getCustomFieldSearcher(scriptedFieldType.getHelper());
		// Create custom field
		if (cfType != null) {
			List<JiraContextNode> contexts = Arrays.asList(GlobalIssueContext.getInstance());
			List<IssueType> issueTypes = Arrays.asList((IssueType) null);
			try {
				CustomField replacementField = CUSTOM_FIELD_MANAGER.createCustomField(
						scriptedField.getName(), 
						scriptedField.getDesc(), 
						cfType, 
						cfSearcher, 
						contexts, 
						issueTypes);
				String s = "Created field: " + replacementField.getId();
				row.getActionLog().getFieldAction().add(s);
				row.setReplacementFieldId(replacementField.getId());
				// Override context and issue types after creation
				if (originalField.getConfigurationSchemes() != null) {
					// Delete default scheme
					if (replacementField.getConfigurationSchemes() != null) {
						for (FieldConfigScheme scheme : replacementField.getConfigurationSchemes()) {
							FIELD_CONFIG_SCHEME_MANAGER.removeFieldConfigScheme(scheme.getId());
						}
					}
					// Recreate schemes
					for (FieldConfigScheme scheme : originalField.getConfigurationSchemes()) {
						FieldConfigScheme.Builder builder = new FieldConfigScheme.Builder();
						FieldConfigScheme newScheme = builder	
								.setName(scheme.getName())
								.setDescription(scheme.getDescription())
								.setFieldId(scheme.getField().getId())
								.toFieldConfigScheme();
						List<IssueType> newIssueTypes = new ArrayList<>();
						for (IssueType type : scheme.getAssociatedIssueTypes()) {
							newIssueTypes.add(type);
						}
						newScheme = FIELD_CONFIG_SCHEME_MANAGER.createFieldConfigScheme(
								newScheme, 
								scheme.getContexts(), 
								newIssueTypes, 
								replacementField);
						s = "Added config scheme: " + newScheme.getId();
						row.getActionLog().getFieldAction().add(s);
					}
				}
			} catch (Exception ex) {
				String s = "Failed: " + ex.getMessage();
				row.getActionLog().getFieldAction().add(s);
			}
		} else {
			String s = "No replacement field available for " + scriptedFieldType;
			row.getActionLog().getFieldAction().add(s);
		}
	}
	
	private void updateScreen(DataRow row, boolean replace) {
		String fromFieldId;
		String toFieldId;
		if (row.getReplacementFieldId() == null) {
			String s = "No replacement field, cannot update screen";
			row.getActionLog().getScreenAction().add(s);
			return;
		}
		if (replace) {
			fromFieldId = row.getScriptedField().getFullFieldId();
			toFieldId = row.getReplacementFieldId();
		} else {
			fromFieldId = row.getReplacementFieldId();
			toFieldId = row.getScriptedField().getFullFieldId();
		}
		// For each usage in screen
		List<ScreenInfo> screenList = sessionData.getUsedInScreens().get(fromFieldId);
		if (screenList != null) {
			for (ScreenInfo screenInfo : screenList) {
				FieldScreenTab tab = FIELD_SCREEN_MANAGER.getFieldScreenTab(screenInfo.getTabId());
				List<FieldScreenLayoutItem> items = FIELD_SCREEN_MANAGER.getFieldScreenLayoutItems(tab);
				// Find original field
				FieldScreenLayoutItem item = null;
				for (FieldScreenLayoutItem entry : items) {
					if (entry.getFieldId().equals(fromFieldId)) {
						item = entry;
						break;
					}
				}
				if (item != null) {
					item.setFieldId(toFieldId);
					FIELD_SCREEN_MANAGER.updateFieldScreenLayoutItem(item);
					String s = "Updating layout item " + item.getId() + ": " + 
							"sequence " + item.getPosition() + " " + 
							"from " + fromFieldId + " " + 
							"to " + toFieldId;
					row.getActionLog().getScreenAction().add(s);
				} else {
					String s = "Unable to locate original field in tab";
					row.getActionLog().getScreenAction().add(s);
				}
			}
		} else {
			String s = "No screen associated with source field: " + fromFieldId;
			row.getActionLog().getScreenAction().add(s);
		}
	}
	
	public String doRestoreSession() throws Exception {
		loadSession();
		getHttpRequest();
		HttpServletRequest wrapperReq = ServletActionContext.getRequest();
		if (wrapperReq instanceof MultiPartRequestWrapper) {
			MultiPartRequestWrapper wrapper = (MultiPartRequestWrapper) wrapperReq;
			File file = wrapper.getFile("file");
			ObjectReader reader = OM.readerFor(SessionData.class);
			SessionData restoreData = reader.readValue(file);
			// Match with current sessionData using field name and type, ignoring id
			for (DataRow currentRow : sessionData.getDataRows().values()) {
				String currentName = currentRow.getScriptedField().getName();
				ScriptedFieldType currentType = currentRow.getScriptedFieldType();
				for (DataRow restoreRow : restoreData.getDataRows().values()) {
					String restoreName = restoreRow.getScriptedField().getName();
					ScriptedFieldType restoreType = restoreRow.getScriptedFieldType();
					if (restoreName.equals(currentName) && 
						restoreType == currentType) {
						currentRow.setActionLog(new ActionLog());
						currentRow.setDataAction(restoreRow.getDataAction());
						currentRow.setFieldAction(restoreRow.getFieldAction());
						currentRow.setProjects(restoreRow.getProjects());
						currentRow.setScreenAction(restoreRow.getScreenAction());
						break;
					}
				}
			}			
			fetchProjects();
			fetchCustomFields();
			fetchUsedInScreens();
			saveSession();
		}
		return JiraWebActionSupport.INPUT;
	}
	
	public String doSaveSession() throws Exception {
		// TODO 
		// When loading the field ids would be different.
		// Can't rely on name either.
		// How to load?
		loadSession();
		try {
			String content = OM.writeValueAsString(sessionData);
			// Prepare download
			HttpServletResponse resp = getHttpResponse();
			resp.setHeader("Content-Type", "plain/text; charset=utf-8");
			resp.setHeader("Content-Disposition", "attachment; filename=\"ScriptedFieldConversion.json\""); 
			try (PrintWriter writer = resp.getWriter()) {
				writer.write(content);
			}
			return JiraWebActionSupport.SUCCESS;
		} catch (Exception ex) {
			return JiraWebActionSupport.INPUT;
		}
	}
	
	public static String getJql(ApplicationUser user, DataRow row) throws Exception {
		String baseJql = "";
		if (row.getProjects() != null && !row.getProjects().isEmpty()) {
			baseJql = "project in (" + row.getProjects() + ") ";
		} 
		String jql = "";
		try {
			jql = "cf[" + row.getScriptedField().getCustomFieldId() + "] IS NOT EMPTY"; 
			if (!baseJql.isEmpty()) {
				jql = jql + " and " + baseJql;
			}
			Query query = JQL_QUERY_PARSER.parseQuery(jql);
			MessageSet msgSet = SEARCH_SERVICE.validateQuery(user, query);
			if (msgSet.getErrorMessages().size() != 0) {
				throw new Exception();
			}
		} catch (Exception ex) {
			// Target specific project keys.
			jql = baseJql;
		}
		if (jql.isEmpty()) {
			throw new Exception("Field does not support IS NOT EMPTY and no project keys defined");
		}
		return jql;
	}
	
	public static SearchResults<Issue> searchIssue(ApplicationUser user, DataRow row, String jql) {
		SearchResults<Issue> result = null;
		try {
			Query query = JQL_QUERY_PARSER.parseQuery(jql);
			result = SEARCH_SERVICE.search(
					user, 
					query, 
					PagerFilter.getUnlimitedFilter());
		} catch (Exception ex) {
			Log.error(LOGGER, "Unable to execute JQL [" + jql + "]", ex);
		}
		return result;
	}
	
	private void copyData(HttpServletRequest req, DataRow row) {
		Job copyJob = new CopyDataJob(getLoggedInUser(), row);
		JobRunnerKey key = JobRunnerKey.of(copyJob.getJobId().toString());
		Map<String, Serializable> parameters = new HashMap<>();
		SCHEDULER_SERVICE.registerJobRunner(key, copyJob);
		Schedule schedule = Schedule.runOnce(new Date());
		JobConfig jobConfig = JobConfig
				.forJobRunnerKey(key)
				.withSchedule(schedule)
				.withRunMode(RunMode.RUN_ONCE_PER_CLUSTER)
				.withParameters(parameters);
		try {
			SCHEDULER_SERVICE.scheduleJob(copyJob.getJobId(), jobConfig);
			row.getActionLog().getDataAction().add("Started copy job: " + copyJob.getJobId());
		} catch (SchedulerServiceException e) {
			Log.error(LOGGER, "Failed to create adhoc job", e);
			row.getActionLog().getDataAction().add("Failed to start copy job: " + e.getMessage());
		}
	}
	
	private void exportData(HttpServletRequest req, DataRow row) {
		Job exportJob = new ExportDataJob(getLoggedInUser(), row);
		JobRunnerKey key = JobRunnerKey.of(exportJob.getJobId().toString());
		Map<String, Serializable> parameters = new HashMap<>();
		SCHEDULER_SERVICE.registerJobRunner(key, exportJob);
		Schedule schedule = Schedule.runOnce(new Date());
		JobConfig jobConfig = JobConfig
				.forJobRunnerKey(key)
				.withSchedule(schedule)
				.withRunMode(RunMode.RUN_ONCE_PER_CLUSTER)
				.withParameters(parameters);
		try {
			SCHEDULER_SERVICE.scheduleJob(exportJob.getJobId(), jobConfig);
			row.getActionLog().getDataAction().add("Started export job: " + exportJob.getJobId());
		} catch (SchedulerServiceException e) {
			Log.error(LOGGER, "Failed to create adhoc job", e);
			row.getActionLog().getDataAction().add("Failed to start export job: " + e.getMessage());
		}
	}
	
	public String doConvert() throws Exception {
		loadSession();
		// Get parameters
		HttpServletRequest req = getHttpRequest();
		sessionData.parseActions(req);
		// Perform actions for each field
		for (DataRow row : sessionData.getDataRows().values()) {
			row.setActionLog(new ActionLog());
			CustomField originalField = CUSTOM_FIELD_MANAGER
					.getCustomFieldObject(row.getScriptedField().getFullFieldId());
			if (originalField == null) {
				String s = "Error: Scripted field not found";
				row.getActionLog().setFieldName(s);
				continue;
			}
			row.getActionLog().setFieldId(row.getScriptedField().getFullFieldId());
			row.getActionLog().setFieldName(row.getScriptedField().getName());
			// Create field
			switch (row.getFieldAction()) {
			case DELETE:
				deleteReplacementField(row);
				break;
			case CREATE:
				createReplacementField(row, originalField);
				break;
			case NONE:
				if (row.getReplacementFieldId() != null && !row.getReplacementFieldId().isEmpty()) {
					row.getActionLog().getFieldAction().add("Linked to: " + row.getReplacementFieldId());
				} else {
					row.getActionLog().getFieldAction().add("N/A");
				}
				break;
			}
			// Data
			switch (row.getDataAction()) {
			case COPY:
				copyData(req, row);
				break;
			case EXPORT:
				// Collect all exported data into a ZIP file
				if (sessionData.getZipOutputStream() == null) {
					sessionData.setupExportData();
				}
				exportData(req, row);
				break;
			case NONE:
				row.getActionLog().getDataAction().add("N/A");
				break;
			}
			// Screen
			switch (row.getScreenAction()) {
			case NONE:
				row.getActionLog().getScreenAction().add("N/A");
				break;
			case REPLACE:
				updateScreen(row, true);
				break;
			case REVERT:
				updateScreen(row, false);
				break;
			}
//			// Clear action
//			row.setFieldAction(FieldAction.NONE);
//			row.setScreenAction(ScreenAction.NONE);
//			row.setDataAction(DataAction.NONE);
		}
		// Close zip output stream if opened
		if (sessionData.getZipOutputStream() != null) {
			try {
				sessionData.getZipOutputStream().close();
			} catch (IOException ioex) {
				Log.error(LOGGER, "Error closing ZIP output stream", ioex);
			}
		}
		// Update info
		fetchProjects();
		fetchCustomFields();
		fetchUsedInScreens();
		// Turn on auto refresh
		sessionData.setAutoRefresh(true);
		saveSession();
		return JiraWebActionSupport.INPUT;
	}
	
	public String doDeleteAll() throws Exception {
		loadSession();
		for (JobEntity entity : Job.loadAllJobEntity()) {
			Job.deleteJobEntity(entity);
		}
		// Turn off auto refresh
		sessionData.setAutoRefresh(false);
		saveSession();
		return JiraWebActionSupport.INPUT;
	}
	
	public String doDelete() throws Exception {
		loadSession();
		HttpServletRequest req = getHttpRequest();
		String id = req.getParameter("id");
		JobEntity entity = Job.loadJobEntity(id);
		if (entity != null) {
			Job.deleteJobEntity(entity);
		}
		return JiraWebActionSupport.INPUT;
	}
	
	public String doDownload() throws Exception {
		loadSession();
		HttpServletRequest req = getHttpRequest();
		String id = req.getParameter("id");
		JobEntity entity = Job.loadJobEntity(id);
		if (entity != null) {
			try {
				// Prepare download
				HttpServletResponse resp = getHttpResponse();
				resp.setHeader("Content-Type", "application/zip");
				resp.setHeader("Content-Disposition", "attachment; filename=\"" + entity.getScriptedFieldId() + ".zip\""); 
				try (OutputStream out = resp.getOutputStream()) {
					String b64 = entity.getDownload();
					out.write(Base64.getDecoder().decode(b64));
				}
				return JiraWebActionSupport.SUCCESS;
			} catch (Exception ex) {
				return JiraWebActionSupport.INPUT;
			}
		}
		return JiraWebActionSupport.INPUT;
	}
	
	public String doSql() throws Exception {
		loadSession();
		HttpServletRequest req = getHttpRequest();
		Map<String, String[]> parameters = req.getParameterMap();
		if (parameters.containsKey("sql") && 
			parameters.get("sql").length != 0) {
			sessionData.setSql(parameters.get("sql")[0]);
			String[] sqlList = sessionData.getSql().split(";");
			StringBuilder sb = new StringBuilder();
			for (String sqlItem : sqlList) {
				if (sqlItem.trim().length() != 0) { 
					final String finalSql = sqlItem;
					String sqlItemResult = DATABASE_ACCESSOR.executeQuery(new ConnectionFunction<String>() {
						@Override
						public String run(DatabaseConnection dbConn) {
							Connection conn = dbConn.getJdbcConnection(); 
							try (PreparedStatement ps = conn.prepareStatement(finalSql)) {
								boolean hasResultSet = ps.execute();
								if (hasResultSet) {
									ResultSet rs = ps.getResultSet();
									if (rs != null) {
										ResultSetMetaData metadata = rs.getMetaData();
										List<Map<String, String>> resultData = new ArrayList<>();
										while (rs.next()) {
											Map<String, String> rowData = new HashMap<>();
											for (int i = 1; i <= metadata.getColumnCount(); i++) {
												String key = metadata.getColumnName(i);
												String value = rs.getString(i);
												rowData.put(key, value);
											}
											resultData.add(rowData);
										}
										try {
											return OM.writeValueAsString(resultData);
										} catch (JsonProcessingException e) {
											return "JSON error: " + e.getMessage();
										}
									} else {
										return "Unable to retrieve result set";
									}
								} else {
									return "Update count: " + ps.getUpdateCount();
								}
							} catch (SQLException sqlex) {
								return "SQL error: " + sqlex.getMessage();
							}
						}
					});
					sb.append(sqlItemResult).append("\n");
				} else {
					sb.append("SQL is empty").append("\n");
				}
				sb.append("========================================\n");
			}
			sessionData.setSqlResult(sb.toString());
		} else {
			sessionData.setSqlResult("");
		}
		saveSession();
		return JiraWebActionSupport.INPUT;
	}
	
	@Override
	protected String doExecute() throws Exception {
		loadSession();
		return JiraWebActionSupport.INPUT;
	}
}
