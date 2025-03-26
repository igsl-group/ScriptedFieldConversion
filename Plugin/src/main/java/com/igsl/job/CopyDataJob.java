package com.igsl.job;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.UpdateIssueRequest;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.igsl.Log;
import com.igsl.ScriptedFieldConversion;
import com.igsl.action.DataAction;
import com.igsl.search.IssueSearchUtil;
import com.igsl.session.DataRow;

public class CopyDataJob extends Job {

	private static final Logger LOGGER = Logger.getLogger(CopyDataJob.class);
	private static final CustomFieldManager CUSTOM_FIELD_MANAGER = ComponentAccessor.getCustomFieldManager();
	private static final IssueManager ISSUE_MANAGER = ComponentAccessor.getIssueManager();
	
	public CopyDataJob(JobEntity entity) {
		super(entity);
	}
	public CopyDataJob(ApplicationUser user, DataRow row) {
		super(user, row, DataAction.COPY);
	}

	@Override
	public JobRunnerResponse runJob(JobRunnerRequest request) {
		start();
		appendMessage("Copying data");
		CustomField scriptedField = CUSTOM_FIELD_MANAGER
				.getCustomFieldObject(dataRow.getScriptedField().getFullFieldId());
		CustomField targetField = CUSTOM_FIELD_MANAGER
				.getCustomFieldObject(dataRow.getReplacementFieldId());
		if (scriptedField == null || targetField == null) {
			appendMessage("Scripted field or replacement field not found");
			return JobRunnerResponse.failed(this.getJobEntity().getMessage());
		}
		try {
			String jql = ScriptedFieldConversion.getJql(user, dataRow);
			appendMessage("JQL: " + jql);
			Stream<Issue> issues = IssueSearchUtil.streamOf(this.user, jql);
			AtomicInteger updatedCount = new AtomicInteger(0);
			issues.forEach(issue -> {
				try {
					MutableIssue mi = (MutableIssue) ISSUE_MANAGER.getIssueObject(issue.getId());
					// Get value
					Object value = mi.getCustomFieldValue(scriptedField);
					// Convert value if required
					switch (dataRow.getDataConversionType()) {
					case NONE:
						break;
					default: 
						value = dataRow.getDataConversionType().getImplementation().convert(mi, value);
						break;
					}
					// Store value into replacement field
					mi.setCustomFieldValue(targetField, value);
					UpdateIssueRequest updateReq = UpdateIssueRequest.builder()
							.eventDispatchOption(EventDispatchOption.DO_NOT_DISPATCH)
							.build();
					if (ISSUE_MANAGER.updateIssue(
							ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), 
							mi, updateReq) != null) {
						updatedCount.incrementAndGet();
						setCurrentStatus(updatedCount + " issue(s) processed");
					} else {
						appendMessage("Failed to update issue " + issue.getKey());
					}
				} catch (Exception ex) {
					appendMessage("Failed to update issue " + issue.getKey() + ": " + ex.getMessage());
					Log.error(LOGGER, "Failed to update issue " + issue.getKey(), ex);
				}
			});
		} catch (Exception ex) {
			appendMessage("Failed to copy data: " + ex.getMessage());
			Log.error(LOGGER, "Failed to write copy data", ex);
		}
		stop();
		return JobRunnerResponse.success(this.getJobEntity().getMessage());
	}

}
