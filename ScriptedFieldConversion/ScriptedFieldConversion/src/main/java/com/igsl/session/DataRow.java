package com.igsl.session;

import java.util.ArrayList;
import java.util.List;

import com.igsl.action.DataAction;
import com.igsl.action.FieldAction;
import com.igsl.action.ScreenAction;
import com.igsl.dataconversion.DataConversionType;

/**
 * Information about a single scripted field
 */
public class DataRow {
	private ScriptedField scriptedField;
	private ScriptedFieldType scriptedFieldType;
	private FieldAction fieldAction;
	private ScreenAction screenAction;
	private DataAction dataAction;
	private String replacementFieldId;
	private String projects;
	private DataConversionType dataConversionType;
	private ActionLog actionLog = new ActionLog();
	public ScriptedField getScriptedField() {
		return scriptedField;
	}
	public void setScriptedField(ScriptedField scriptedField) {
		this.scriptedField = scriptedField;
	}
	public ScriptedFieldType getScriptedFieldType() {
		return scriptedFieldType;
	}
	public void setScriptedFieldType(ScriptedFieldType scriptedFieldType) {
		this.scriptedFieldType = scriptedFieldType;
	}
	public FieldAction getFieldAction() {
		return fieldAction;
	}
	public void setFieldAction(FieldAction fieldAction) {
		this.fieldAction = fieldAction;
	}
	public ScreenAction getScreenAction() {
		return screenAction;
	}
	public void setScreenAction(ScreenAction screenAction) {
		this.screenAction = screenAction;
	}
	public DataAction getDataAction() {
		return dataAction;
	}
	public void setDataAction(DataAction dataAction) {
		this.dataAction = dataAction;
	}
	public String getReplacementFieldId() {
		return replacementFieldId;
	}
	public void setReplacementFieldId(String replacementFieldId) {
		this.replacementFieldId = replacementFieldId;
	}
	public ActionLog getActionLog() {
		return actionLog;
	}
	public void setActionLog(ActionLog actionLog) {
		this.actionLog = actionLog;
	}
	public String getProjects() {
		return projects;
	}
	public void setProjects(String projects) {
		this.projects = projects;
	}
	public DataConversionType getDataConversionType() {
		return dataConversionType;
	}
	public void setDataConversionType(DataConversionType dataConversion) {
		this.dataConversionType = dataConversion;
	}
}
