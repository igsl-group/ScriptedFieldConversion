package com.igsl.action;

import com.fasterxml.jackson.annotation.JsonValue;

public interface Action<T> {
	public String getParameterName();
	@JsonValue
	public String getValue();
	public String getDisplay();
}