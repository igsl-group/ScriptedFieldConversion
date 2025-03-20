package com.igsl.job;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igsl.Log;
import com.igsl.ScriptedFieldConversion;
import com.igsl.ScriptedFieldConversionConstants;
import com.igsl.action.DataAction;
import com.igsl.dataconversion.DataConversion;
import com.igsl.importconfig.ImportConfig;
import com.igsl.session.DataRow;

public class ExportDataJob extends Job {

	private static final ObjectMapper OM = new ObjectMapper();
	private static final Logger LOGGER = Logger.getLogger(ExportDataJob.class);
	
	public ExportDataJob(JobEntity entity) {
		super(entity);
	}
	public ExportDataJob(ApplicationUser user, DataRow row) {
		super(user, row, DataAction.EXPORT);
	}

	private static final CustomFieldManager CUSTOM_FIELD_MANAGER = ComponentAccessor.getCustomFieldManager();
	
	@Override
	public JobRunnerResponse runJob(JobRunnerRequest request) {
		start();
		appendMessage("Exporting data");
		CustomField scriptedField = CUSTOM_FIELD_MANAGER.getCustomFieldObject(this.getJobEntity().getScriptedFieldId());
		if (scriptedField == null) {
			appendMessage("Scripted field not found");
		} else {
			try (	ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
					ZipOutputStream zos = new ZipOutputStream(baos)) {
				// Add configuration file
				zos.putNextEntry(new ZipEntry("Config.json"));
				zos.write(OM.writeValueAsBytes(new ImportConfig(scriptedField.getName())));
				zos.closeEntry();
				// Search for issues
				String jql = ScriptedFieldConversion.getJql(user, dataRow);
				appendMessage("JQL: " + jql);
				SearchResults<Issue> searchResult = ScriptedFieldConversion.searchIssue(
						this.user,
						this.dataRow,
						jql);
				appendMessage("Issues found: " + searchResult.getResults().size());
				try {
					zos.putNextEntry(new ZipEntry(dataRow.getScriptedField().getFullFieldId() + ".csv"));
					// Due to Jira having out-dated Apache commons IO/lang3
					// And Jira's OGSI framework blocking usage of newer versions
					// Apache Commons CSV cannot be used here
					// Write header
					StringBuilder headers = new StringBuilder();
					for (String header : ScriptedFieldConversionConstants.CSV_HEADERS) {
						headers.append(",").append(DOUBLE_QUOTE).append(header).append(DOUBLE_QUOTE);
					}
					headers.append(NEWLINE);
					headers.delete(0, 1);	// Remove initial comma
					zos.write(headers.toString().getBytes());
					for (Issue issue : searchResult.getResults()) {
						setCurrentStatus("Processing issue " + issue.getKey());
						// Export data to CSV
						// Importing CSV requires project key, name, type, summary and issue key
						String projectKey = issue.getProjectObject().getKey();
						String projectName = issue.getProjectObject().getName();
						String projectType = issue.getProjectObject().getProjectTypeKey().getKey();
						String summary = issue.getSummary();
						String issueKey = issue.getKey();
						String issueType = issue.getIssueType().getName();
						Object fieldValue = issue.getCustomFieldValue(scriptedField);
						// Apply data conversion	
						Object convertedFieldValue = null;
						switch (dataRow.getDataConversionType()) {
						case NONE:
							convertedFieldValue = fieldValue;
							break;
						default: 
							DataConversion conv = dataRow.getDataConversionType().getImplementation();
							if (conv != null) {
								try {
									convertedFieldValue = conv.convert(issue, fieldValue);
									Log.debug(LOGGER, 
											"Issue " + issue.getKey() + 
											" Value converted by " + dataRow.getDataConversionType() + 
											" From: " + fieldValue + 
											" To: " + convertedFieldValue);
								} catch (Exception ex) {
									appendMessage(
											"Issue " + issue.getKey() + 
											" Unable to convert value using " + dataRow.getDataConversionType() + 
											": " + ex.getMessage());
								}
							} else {
								appendMessage(
										"Issue " + issue.getKey() + 
										" Unrecognized data conversion: " + dataRow.getDataConversionType());
							}
							break;
						}
						String fieldValueString = "";
						if (fieldValue != null) {
							if (fieldValue instanceof String) {
								fieldValueString = String.valueOf(fieldValue);
							} else {
								fieldValueString = OM.writeValueAsString(fieldValue);
							}
						}
						String convertedFieldValueString = "";
						if (convertedFieldValue != null) {
							if (convertedFieldValue instanceof String) {
								convertedFieldValueString = String.valueOf(convertedFieldValue);
							} else {
								convertedFieldValueString = OM.writeValueAsString(convertedFieldValue);
							}
						}
						fieldValueString = fieldValueString
								.replaceAll(DOUBLE_QUOTE, DOUBLE_QUOTE + DOUBLE_QUOTE);
						convertedFieldValueString = convertedFieldValueString
								.replaceAll(DOUBLE_QUOTE, DOUBLE_QUOTE + DOUBLE_QUOTE);
						zos.write((
								DOUBLE_QUOTE + projectKey + DOUBLE_QUOTE + "," + 
								DOUBLE_QUOTE + projectName + DOUBLE_QUOTE + "," + 
								DOUBLE_QUOTE + projectType + DOUBLE_QUOTE + "," + 
								DOUBLE_QUOTE + summary + DOUBLE_QUOTE + "," + 
								DOUBLE_QUOTE + issueKey + DOUBLE_QUOTE + "," + 
								DOUBLE_QUOTE + issueType + DOUBLE_QUOTE + "," + 
								DOUBLE_QUOTE + dataRow.getScriptedField().getFullFieldId() + DOUBLE_QUOTE + "," + 
								DOUBLE_QUOTE + dataRow.getScriptedField().getName() + DOUBLE_QUOTE + "," + 
								DOUBLE_QUOTE + fieldValueString + DOUBLE_QUOTE + "," + 
								DOUBLE_QUOTE + dataRow.getDataConversionType().getValue() + DOUBLE_QUOTE + "," + 
								DOUBLE_QUOTE + convertedFieldValueString + DOUBLE_QUOTE + NEWLINE).getBytes());
						setCurrentStatus("Issue " + issue.getKey() + " processed");
					}	// For each issue found
					zos.closeEntry();
				} catch (Exception ex) {
					appendMessage("Failed to write ZIP entry: " + ex.getMessage());
					Log.error(LOGGER, "Failed to write ZIP entry", ex);
				}
				zos.close();
				// Store output
				String download = Base64.getEncoder().encodeToString(baos.toByteArray());
				getJobEntity().setDownload(download);
				appendMessage("Download prepared");
			} catch (Exception ex) {
				appendMessage("Failed to write ZIP file: " + ex.getMessage());
				Log.error(LOGGER, "Failed to write ZIP file", ex);
			}
		}
		stop();
		return JobRunnerResponse.success(this.getJobEntity().getMessage());
	}

}
