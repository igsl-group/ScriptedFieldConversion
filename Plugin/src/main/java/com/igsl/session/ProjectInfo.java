package com.igsl.session;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProjectInfo {
	
	public static final String QUERY = 
			"SELECT DISTINCT * FROM (" + 
			"	SELECT" + 
			"		cf.ID AS cfid" + 
			"		, cf.cfname AS cfname" + 
			"		, cf.CUSTOMFIELDTYPEKEY AS cftype" + 
			"		, COALESCE(p.pkey, 'Global') AS projectkey" + 
			"		, COALESCE(p.id, '0') AS projectid" + 
			"		, p.pname AS projectname" + 
			"	FROM " + 
			"		customfield cf " + 
			"		JOIN fieldscreenlayoutitem fsli ON fsli.FIELDIDENTIFIER = CONCAT('customfield_', cf.ID)" + 
			"		JOIN fieldscreentab fst ON fst.ID = fsli.FIELDSCREENTAB" + 
			"		JOIN fieldscreen fs ON fs.ID = fst.FIELDSCREEN" + 
			"		JOIN fieldscreenschemeitem fssi ON fssi.FIELDSCREEN = fs.ID" + 
			"		JOIN fieldscreenscheme fss ON fss.ID = fssi.FIELDSCREENSCHEME" + 
			"		JOIN issuetypescreenschemeentity itsse ON itsse.FIELDSCREENSCHEME = fss.ID " + 
			"		JOIN issuetypescreenscheme itss ON itss.ID = itsse.SCHEME" + 
			"		LEFT JOIN issuetype it ON it.ID = itsse.ISSUETYPE" + 
			"		JOIN nodeassociation naScreen " + 
			"			ON naScreen.SINK_NODE_ID = itss.ID AND naScreen.SINK_NODE_ENTITY = 'IssueTypeScreenScheme'" + 
			"		LEFT JOIN project p " + 
			"			ON p.ID = naScreen.SOURCE_NODE_ID AND naScreen.SINK_NODE_ENTITY = 'IssueTypeScreenScheme'" + 
			"	WHERE " + 
			"		cf.CUSTOMFIELDTYPEKEY LIKE 'com.onresolve.jira.groovy.groovyrunner:%' " + 
			"	UNION ALL" + 
			"	SELECT" + 
			"		cf.ID AS cfid" + 
			"		, cf.cfname AS cfname" + 
			"		, cf.CUSTOMFIELDTYPEKEY as cftype" + 
			"		, COALESCE(p.pkey, 'Global') AS projectkey" + 
			"		, COALESCE(p.id, '0') AS projectid" + 
			"		, p.pname AS projectname" + 
			"	FROM " + 
			"		customfield cf" + 
			"		JOIN fieldscreenlayoutitem fsli ON fsli.FIELDIDENTIFIER = CONCAT('customfield_', cf.ID)" + 
			"		JOIN fieldscreentab fst ON fst.ID = fsli.FIELDSCREENTAB" + 
			"		JOIN fieldscreen fs ON fs.ID = fst.FIELDSCREEN" + 
			"		JOIN jiraworkflows wf " + 
			"			ON EXTRACTVALUE(wf.DESCRIPTOR, '//meta[@name = \"jira.fieldscreen.id\"]/text()') " + 
			"			RLIKE CONCAT('(^| )', fs.ID, '( |$)')" + 
			"		JOIN workflowschemeentity wse ON wse.WORKFLOW = wf.workflowname" + 
			"		JOIN workflowscheme ws ON ws.ID = wse.SCHEME" + 
			"		LEFT JOIN issuetype it ON it.ID = wse.ISSUETYPE" + 
			"		JOIN nodeassociation naWorkflow " + 
			"			ON naWorkflow.SINK_NODE_ID = ws.ID AND naWorkflow.SINK_NODE_ENTITY = 'WorkflowScheme'" + 
			"		JOIN project p " + 
			"			ON p.ID = naWorkflow.SOURCE_NODE_ID AND naWorkflow.SINK_NODE_ENTITY = 'WorkflowScheme'" + 
			"	WHERE " + 
			"		cf.CUSTOMFIELDTYPEKEY LIKE 'com.onresolve.jira.groovy.groovyrunner:%' " +
			") tmp";
	
	private String fieldId;
	private String projectName;
	private String projectKey;
	private Long projectId;
	public ProjectInfo(ResultSet rs) throws SQLException {
		this.fieldId = rs.getString("cfid");
		this.projectName = rs.getString("projectname");
		this.projectId = rs.getLong("projectid");
		this.projectKey = rs.getString("projectkey");
	}
	public String getProjectName() {
		return projectName;
	}
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public String getProjectKey() {
		return projectKey;
	}
	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}
	public Long getProjectId() {
		return projectId;
	}
	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}
	public String getFieldId() {
		return fieldId;
	}
	public void setFieldId(String fieldId) {
		this.fieldId = fieldId;
	}
}
