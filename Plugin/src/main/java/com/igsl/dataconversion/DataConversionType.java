package com.igsl.dataconversion;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.igsl.action.Action;
import com.igsl.action.ActionHandler;

public enum DataConversionType implements Action<DataConversionType> {
	NONE("N/A", "", null), 
	ISSUE_PICKER(IssuePicker.NAME, IssuePicker.DESC, new IssuePicker()),
	QUOTE_RESPONSES(QuoteResponses.NAME, QuoteResponses.DESC, new QuoteResponses());	
	private String name;
	private String desc;
	private DataConversion implementation;
	private DataConversionType(String name, String desc, DataConversion cls) {
		this.name = name;
		this.desc = desc;
		this.implementation = cls;
	}
	@JsonCreator
	public static DataConversionType parse(String s) {
		return ActionHandler.parse(DataConversionType.class, s, NONE);
	}
	@Override
	public String getParameterName() {
		return "dataConversion";
	}
	@Override
	public String getValue() {
		if (implementation != null) {
			return implementation.getClass().getCanonicalName();
		} 
		return " ";
	}
	@Override
	public String getDisplay() {
		return name;
	}
	public DataConversion getImplementation() {
		return implementation;
	}
	public String getDesc() {
		return desc;
	}
}
