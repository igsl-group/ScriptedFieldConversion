package com.igsl;

/**
 * Constant values for custom field type and searcher
 * 
 * Reference: 
 * https://confluence.atlassian.com/jirakb/jira-software-rest-api-essential-parameters-for-custom-field-creation-1295815501.html
 * 
 * Note:
 * Not all types are added to this enum
 */
public enum CustomFieldTypeHelper {
	CHECKBOX("multicheckboxes", "multiselectsearcher"),
	DATE_PICKER("datepicker", "daterange"),
	DATETIME_PICKER("datetime", "datetimerange"),
	GROUP_PICKER_MULTIPLE("multigrouppicker", "grouppickersearcher"),
	GROUP_PICKER_SINGLE("grouppicker", "grouppickersearcher"),
	NUMBER("float", "exactnumber"),
	PROJECT_PICKER("project", "projectsearcher"),
	CASCADING_LIST("cascadingselect", "cascadingselectsearcher"),
	MULTISELECT_LIST("multiselect", "multiselectsearcher"),
	SELECT_LIST("select", "multiselectsearcher"),
	TEXT_AREA("textarea", "textsearcher"),
	TEXT_READ_ONLY("readonlyfield", "textsearcher"),
	TEXT_FIELD("textfield", "textsearcher"),
	URL_FIELD("url", "exacttextsearcher"),
	MULTIUSER_PICKER("multiuserpicker", "userpickergroupsearcher"),
	USER_PICKER("userpicker", "userpickergroupsearcher");	
	private static final String TYPE_PREFIX = "com.atlassian.jira.plugin.system.customfieldtypes:";
	private String typeKey;
	private String searcherKey;
	private CustomFieldTypeHelper(String typeKey, String searcherKey) {
		this.typeKey = TYPE_PREFIX + typeKey;
		this.searcherKey = TYPE_PREFIX + searcherKey;
	}
	public String getTypeKey() {
		return typeKey;
	}
	public String getSearcherKey() {
		return searcherKey;
	}
}
