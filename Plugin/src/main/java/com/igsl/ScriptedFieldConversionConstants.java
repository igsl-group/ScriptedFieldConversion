package com.igsl;

/**
 * Stores constant values for ScriptedFieldConversion.
 * Since from ScriptedFieldConversionCSVMapper, it is not possible to create ScriptedFieldConversion instances.
 */
public class ScriptedFieldConversionConstants {
	
	public static final String CSV_HEADER_DATA_CONVERSION = "DataConversion";
	public static final String CSV_HEADER_CONVERTED_VALUE = "Converted Value";

	public static final String[] CSV_HEADERS = {
		"Project Key",
		"Project Name", 
		"Project Type",
		"Summary",
		"Issue Key",
		"Issue Type", 
		"Field Id",
		"Field Name",
		"Field Value",
		CSV_HEADER_DATA_CONVERSION,
		CSV_HEADER_CONVERTED_VALUE
	};
	
	public static final int CSV_COLUMN_INDEX_CONVERTED_VALUE = CSV_HEADERS.length - 1;
}
