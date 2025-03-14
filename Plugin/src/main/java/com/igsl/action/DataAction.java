package com.igsl.action;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DataAction implements Action<DataAction> {
	NONE("none", "N/A"),
	COPY("copy", "Copy"),
	EXPORT("export", "Export CSV");	
	private String value;
	private String display;
	private DataAction(String value, String display) {
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
		return "dataAction";
	}
	@JsonCreator
	public static DataAction parse(String s) {
		return ActionHandler.parse(DataAction.class, s, NONE);
	}
}
