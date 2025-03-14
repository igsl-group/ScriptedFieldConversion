package com.igsl.action;

import java.util.HashMap;
import java.util.Map;

public class ActionHandler {
	
	private static final String SELECT_PARAM = "select";
	
	public static <T extends Action<T>> T getInstance(Class<T> enumType) {
		return enumType.getEnumConstants()[0];
	}
	
	public static <T extends Action<T>> T parse(Class<T> enumType, String s, T defaultValue) {
		if (s != null) {
			for (T c : enumType.getEnumConstants()) {
				if (c.getValue().equals(s)) {
					return c;
				}
			}
		}
		return defaultValue;
	}
	
	public static Map<String, String> parseParameter(String name, Map<String, String[]> parameters) {
		Map<String, String> result = new HashMap<>();
		String[] fieldList = parameters.get(SELECT_PARAM);
		String[] paramList = parameters.get(name);
		if (fieldList != null && paramList != null) {
			for (int i = 0; i < fieldList.length; i++) {
				result.put(fieldList[i], paramList[i]);
			}
		}
		return result;
	}
	
	public static <T extends Action<T>> Map<String, T> parseAction(
			Class<T> enumType, Map<String, String[]> parameters, T defaultValue) {
		Map<String, T> result = new HashMap<>();
		T e = ActionHandler.getInstance(enumType);
		String[] fieldList = parameters.get(SELECT_PARAM);
		String[] actionList = parameters.get(e.getParameterName());
		if (fieldList != null && actionList != null) {
			for (int i = 0; i < fieldList.length; i++) {
				result.put(fieldList[i], ActionHandler.parse(enumType, actionList[i], defaultValue));
			}
		}
		return result;
	}
}
