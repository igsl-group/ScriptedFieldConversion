package com.igsl.action;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FieldAction implements Action<FieldAction> {
	NONE("none", "N/A"),
	CREATE_WITH_PREFIX("createPrefix", "Create with Prefix"),
	CREATE("create", "Create"),
	DELETE("delete", "Delete");
	
	// TODO Add Delete Replacement
	
	private String value;
	private String display;
	private FieldAction(String value, String display) {
		this.value = value;
		this.display = display;
	}
	@Override
	public String getValue() {
		return value;
	}
	@Override
	public String getDisplay() {
		return display;
	}
	@Override
	public String getParameterName() {
		return "fieldAction";
	}
	@JsonCreator
	public static FieldAction parse(String s) {
		return ActionHandler.parse(FieldAction.class, s, NONE);
	}
}
