package com.igsl.session;

import java.util.ArrayList;
import java.util.List;

public enum FieldType {
	ELEMENTS_CONNECT_LIVE("Elements Connect - Live"),
	ELEMENTS_CONNECT_SNAPSHOT("Elements Connect - Snapshot"),
	SCRIPTED_FIELD("ScriptRunner Scripted Field");
	private String display;
	private FieldType(String display) {
		this.display = display;
	}
	public String getDisplay() {
		return display;
	}
	public static List<FieldType> parse(String[] params) {
		List<FieldType> result = new ArrayList<>();
		if (params != null) {
			for (String param : params) {
				FieldType ft = FieldType.valueOf(param);
				if (ft != null) {
					result.add(ft);
				}
			}
		}
		return result;
	}
}
