package com.igsl;

import java.util.Arrays;
import java.util.List;

/**
 * Stores constant values for ScriptedFieldConversion.
 * Since from ScriptedFieldConversionCSVMapper, it is not possible to create ScriptedFieldConversion instances.
 */
public class ScriptedFieldConversionConstants {
	
	public static final String CSV_HEADER_DATA_CONVERSION = "DataConversion";
	public static final String CSV_HEADER_CONVERTED_VALUE = "Converted Value";
	public static final String CSV_HEADER_FIELD_NAME = "Field Name";
	public static final String CSV_HEADER_ISSUE_KEY = "Issue Key";

	public static final String JIRA_FIELD = "jira.field";
	
	public static final List<String> CSV_HEADERS = Arrays.asList(
		"Project Key",
		"Project Name", 
		"Project Type",
		"Summary",
		CSV_HEADER_ISSUE_KEY,
		"Issue Type", 
		"Field Id",
		CSV_HEADER_FIELD_NAME,
		"Field Value",
		CSV_HEADER_DATA_CONVERSION,
		CSV_HEADER_CONVERTED_VALUE
	);
	
	public static final int CSV_COLUMN_INDEX_ISSUE_KEY = 
			CSV_HEADERS.indexOf(CSV_HEADER_ISSUE_KEY);
	public static final int CSV_COLUMN_INDEX_CONVERTED_VALUE = 
			CSV_HEADERS.indexOf(CSV_HEADER_CONVERTED_VALUE);
}
