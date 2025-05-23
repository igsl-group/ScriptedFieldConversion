package com.igsl.job;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.util.JiraHome;
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
import com.igsl.search.IssueSearchUtil;
import com.igsl.session.DataRow;
import com.igsl.wrapper.JiraObjectWrapper;

public class ExportDataJob extends Job {

	private static final ObjectMapper OM = new ObjectMapper();
	private static final Logger LOGGER = Logger.getLogger(ExportDataJob.class);
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd-HHmmss");
	
	public ExportDataJob(JobEntity entity) {
		super(entity);
	}
	public ExportDataJob(ApplicationUser user, DataRow row) {
		super(user, row, DataAction.EXPORT);
	}

	private static final CustomFieldManager CUSTOM_FIELD_MANAGER = ComponentAccessor.getCustomFieldManager();
	private static final JiraHome JIRA_HOME = ComponentAccessor.getComponent(JiraHome.class);
	
	private String csvEscape(String s) {
		if (s != null) {
			// Double-up double quotes
			s = s.replaceAll(DOUBLE_QUOTE, DOUBLE_QUOTE + DOUBLE_QUOTE);
			return s;
		}
		return null;
	}
	
	@Override
	public JobRunnerResponse runJob(JobRunnerRequest request) {
		start();
		appendMessage("Exporting data");
		// Create directory if not already present
		Path exportDir = Paths.get(JIRA_HOME.getExportDirectory().toString(), "ScriptedFieldConversion");
		if (!Files.exists(exportDir)) {
			try {
				Files.createDirectories(exportDir);
			} catch (IOException e) {
				appendMessage(
						"Unable to create export subdirectory " + exportDir + ": " + 
						e.getMessage());
				stop();
				return JobRunnerResponse.failed(
							"Unable to create export subdirectory " + exportDir + ": " + 
							e.getMessage());
			}
		}
		Path outputFile = exportDir.resolve(
				this.getJobEntity().getScriptedFieldId() + 
				"." + SDF.format(new Date()) + 
				".zip");
		CustomField scriptedField = CUSTOM_FIELD_MANAGER.getCustomFieldObject(this.getJobEntity().getScriptedFieldId());
		if (scriptedField == null) {
			appendMessage("Scripted field not found");
		} else {
			try (	FileOutputStream fos = new FileOutputStream(outputFile.toFile()); 
					ZipOutputStream zos = new ZipOutputStream(fos)) {
				// Add configuration file
				zos.putNextEntry(new ZipEntry("Config.json"));
				zos.write(OM.writeValueAsBytes(new ImportConfig(scriptedField.getName())));
				zos.closeEntry();
				// Search for issues
				String jql = ScriptedFieldConversion.getJql(user, dataRow);
				appendMessage("JQL: " + jql);
				Stream<Issue> issues = IssueSearchUtil.streamOf(user, jql);
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
					AtomicInteger processedCount = new AtomicInteger(0);
					issues.forEach(
						issue -> {
							try {
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
										fieldValueString = OM.writeValueAsString(JiraObjectWrapper.wrap(fieldValue));
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
								zos.write((
										DOUBLE_QUOTE + csvEscape(projectKey) + DOUBLE_QUOTE + "," + 
										DOUBLE_QUOTE + csvEscape(projectName) + DOUBLE_QUOTE + "," + 
										DOUBLE_QUOTE + csvEscape(projectType) + DOUBLE_QUOTE + "," + 
										DOUBLE_QUOTE + csvEscape(summary) + DOUBLE_QUOTE + "," + 
										DOUBLE_QUOTE + csvEscape(issueKey) + DOUBLE_QUOTE + "," + 
										DOUBLE_QUOTE + csvEscape(issueType) + DOUBLE_QUOTE + "," + 
										DOUBLE_QUOTE + csvEscape(dataRow.getScriptedField().getFullFieldId()) + DOUBLE_QUOTE + "," + 
										DOUBLE_QUOTE + csvEscape(dataRow.getScriptedField().getName()) + DOUBLE_QUOTE + "," + 
										DOUBLE_QUOTE + csvEscape(fieldValueString) + DOUBLE_QUOTE + "," + 
										DOUBLE_QUOTE + csvEscape(dataRow.getDataConversionType().getValue()) + DOUBLE_QUOTE + "," + 
										DOUBLE_QUOTE + csvEscape(convertedFieldValueString) + DOUBLE_QUOTE + NEWLINE).getBytes());
								processedCount.incrementAndGet();
								setCurrentStatus(processedCount + " issue(s) processed");
							} catch (Exception ex) {
								appendMessage("Failed to write ZIP entry: " + ex.getMessage());
								Log.error(LOGGER, "Failed to write ZIP entry", ex);
							}
						}
					);
					zos.closeEntry();
				} catch (Exception ex) {
					appendMessage("Failed to write ZIP entry: " + ex.getMessage());
					Log.error(LOGGER, "Failed to write ZIP entry", ex);
				}
				zos.close();
				// Store output
				String download = outputFile.toString();
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
