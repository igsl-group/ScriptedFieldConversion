package com.igsl.session;

import java.util.ArrayList;
import java.util.List;

public class ActionLog {
	private String fieldName;
	private String fieldId;
	private List<String> fieldAction = new ArrayList<>();
	private List<String> dataAction = new ArrayList<>();
	private List<String> screenAction = new ArrayList<>();
	public String getFieldName() {
		return fieldName;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	public String getFieldId() {
		return fieldId;
	}
	public void setFieldId(String fieldId) {
		this.fieldId = fieldId;
	}
	public List<String> getFieldAction() {
		return fieldAction;
	}
	public void setFieldAction(List<String> fieldAction) {
		this.fieldAction = fieldAction;
	}
	public List<String> getDataAction() {
		return dataAction;
	}
	public void setDataAction(List<String> dataAction) {
		this.dataAction = dataAction;
	}
	public List<String> getScreenAction() {
		return screenAction;
	}
	public void setScreenAction(List<String> screenAction) {
		this.screenAction = screenAction;
	}
	

}
