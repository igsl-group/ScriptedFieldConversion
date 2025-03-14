package com.igsl.action;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ScreenAction implements Action<ScreenAction> {
	NONE("none", "N/A"),
	REPLACE("replace", "Replace"),
	REVERT("revert", "Revert");
	private String value;
	private String display;
	private ScreenAction(String value, String display) {
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
		return "screenAction";
	}
	@JsonCreator
	public static ScreenAction parse(String s) {
		return ActionHandler.parse(ScreenAction.class, s, NONE);
	}
}
