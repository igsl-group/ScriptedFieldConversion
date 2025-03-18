package com.igsl.model;

public class CustomField {
	private String id;
	private String name;
	private boolean custom;
	private Schema schema;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isCustom() {
		return custom;
	}
	public void setCustom(boolean custom) {
		this.custom = custom;
	}
	public Schema getSchema() {
		return schema;
	}
	public void setSchema(Schema schema) {
		this.schema = schema;
	}
}
