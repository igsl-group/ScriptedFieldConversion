package com.igsl.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Tokenized<T> extends Pagination<T> {

	public static final String DEFAULT_NEXT_PAGE_TOKEN = "nextPageToken";
	public static final String DEFAULT_MAX_RESULTS = "maxResults";
	public static final String DEFAULT_VALUES = "values";
	
	private String nextPageTokenParameterName = DEFAULT_NEXT_PAGE_TOKEN;
	private String maxResultsParameterName = DEFAULT_MAX_RESULTS;
	private String valuesParameterName = DEFAULT_VALUES;

	private String nextPageToken = null;
	private boolean hasMore;
	private Integer maxResults;
	private Response response;
	private List<T> values;
	
	/**
	 * Constructor.
	 * @param dataClass
	 */
	public Tokenized(Class<T> dataClass) {
		super(dataClass);
	}
	
	public Tokenized(Class<T> dataClass, 
			String tokenParameterName, 
			String maxResults, 
			String values) {
		super(dataClass);
		maxResultsProperty(maxResults);
		valuesProperty(values);
	}
	
	/**
	 * Set query parameter name for max no. of results.
	 * Default is {@value #DEFAULT_MAX_RESULTS}.
	 * @param maxResultsParameterName
	 */
	public Tokenized<T> maxResultsProperty(String maxResultsParameterName) {
		this.maxResultsParameterName = maxResultsParameterName;
		return this;
	}
	
	/**
	 * Set max no. of results.
	 * Default is {@literal null}.
	 * @param maxResults
	 */
	public Tokenized<T> maxResults(Integer maxResults) {
		this.maxResults = maxResults;
		return this;
	}

	/**
	 * Set JSON property name containing next page token.
	 * Default is {@value #DEFAULT_NEXT_PAGE_TOKEN}.
	 * @param nextPageTokenParameterName
	 */
	public Tokenized<T> nextPageTokenProperty(String nextPageTokenParameterName) {
		this.nextPageTokenParameterName = nextPageTokenParameterName;
		return this;
	}

	/**
	 * Set JSON property name containing value or array of values.
	 * Default is {@value #DEFAULT_VALUES}.
	 * @param valuesParameterName
	 */
	public Tokenized<T> valuesProperty(String valuesParameterName) {
		this.valuesParameterName = valuesParameterName;
		return this;
	}

	@Override
	public void reset() {
		this.response = null;
		this.nextPageToken = null;
		this.values = null;
	}

	@Override
	public void setup(RestUtil<?> util) {
		if (this.nextPageTokenParameterName != null && 
			!this.nextPageTokenParameterName.isEmpty() && 
			this.nextPageToken != null && 
			!this.nextPageToken.isEmpty()) {
			util.query(this.nextPageTokenParameterName, this.nextPageToken);
		}
		if (this.maxResultsParameterName != null && 
			!this.maxResultsParameterName.isEmpty() && 
			this.maxResults != null) {
			util.query(this.maxResultsParameterName, this.maxResults);
		}
	}

	@Override
	public void setResponse(Response response, ObjectMapper om) 
			throws JsonProcessingException, IllegalArgumentException, IOException {
		this.response = response;
		// Parse response
		String jsonString = this.response.readEntity(String.class);		
		JsonNode root = om.readTree(jsonString);
		// nextPageToken
		nextPageToken = null;
		if (this.nextPageTokenParameterName != null && !this.nextPageTokenParameterName.isEmpty()) {
			JsonNode totalNode = root.get(nextPageTokenParameterName);
			if (totalNode != null && totalNode.isInt()) {
				nextPageToken = totalNode.asText();
			}
		}
		// Values
		if (valuesParameterName != null && !valuesParameterName.isEmpty()) {
			JsonNode valuesNode = root.get(valuesParameterName);
			if (valuesNode != null) {
				this.values = new ArrayList<>();
				if (valuesNode.isArray()) {
					// Array
					for (JsonNode arrayItem : valuesNode) {
						T item = om.treeToValue(arrayItem, dataClass);
						this.values.add(item);
					}
				} else {
					// Single item
					T item = om.treeToValue(valuesNode, dataClass);
					this.values.add(item);
				}
	 		} else {
	 			this.values = null;
	 		}
		} else {
			// Root should be array of values
			if (root.isArray()) {
				this.values = new ArrayList<>();
				for (JsonNode arrayItem : root) {
					T item = om.treeToValue(arrayItem, dataClass);
					this.values.add(item);
				}
			} else if (root.isObject()) {
				// Single item
				this.values = new ArrayList<>();
				T item = om.treeToValue(root, dataClass);
				this.values.add(item);
			} else {
				this.values = null;
			}
		}
		// Has more
		this.hasMore = (nextPageToken != null && !nextPageToken.isEmpty());
	}
	
	@Override
	public boolean hasMore() {
		return this.hasMore;
	}

	@Override
	public List<T> getObjects() {
		return this.values;
	}
}
