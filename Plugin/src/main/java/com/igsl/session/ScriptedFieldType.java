package com.igsl.session;

import com.igsl.CustomFieldTypeHelper;

public enum ScriptedFieldType {
	ELEMENTS_CONNECT("elementsConnect", CustomFieldTypeHelper.TEXT_FIELD),
	FLOAT("float", CustomFieldTypeHelper.NUMBER),
	TEXTAREA("textarea", CustomFieldTypeHelper.TEXT_AREA),
	ISSUEPICKER("IssuePickerCommand", null),
	HTML("html", CustomFieldTypeHelper.TEXT_AREA),
	DURATION("duration", null),
	USERPICKER("userpicker", CustomFieldTypeHelper.USER_PICKER),
	MULTIUSERPICKER("multiuserpicker", CustomFieldTypeHelper.MULTIUSER_PICKER),
	DATE("date", CustomFieldTypeHelper.DATE_PICKER),
	PARENTISSUE("ParentIssueScriptFieldCommand", null);
	private String value;
	private CustomFieldTypeHelper helper;
	private ScriptedFieldType(String value, CustomFieldTypeHelper helper) {
		this.value = value;
		this.helper = helper;
	}
	public CustomFieldTypeHelper getHelper() {
		return this.helper;
	}
	public static ScriptedFieldType parse(String s) {
		if (s != null) {
			for (ScriptedFieldType type : ScriptedFieldType.values()) {
				if (type.value.equals(s)) {
					return type;
				}
			}
		}
		return null;
	}
}
