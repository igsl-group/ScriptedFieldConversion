package com.igsl.dataconversion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.issue.Issue;
import com.igsl.Log;

/**
 * Data conversion for issue picker. 
 * The data received from Java API is Issue objects.
 * Convert it to comma-delimited issue keys.
 */
public class IssuePicker extends DataConversion {

	public static final String NAME = "Issue Picker";
	public static final String DESC = "Issue Object => Comma-delimited issue keys";
	
	private static final String DELIMITER = ",";
	
	private String _convert(List<Issue> issueList) {
		StringBuilder sb = new StringBuilder();
		for (Issue issue : issueList) {
			sb.append(DELIMITER).append(issue.getKey());
		}
		sb.delete(0, DELIMITER.length());
		return sb.toString();
	}
	
	private String _convert(Issue issue) {
		if (issue != null) {
			return issue.getKey();
		} 
		return "";
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object convert(
			Issue issue, 
			Object sourceValue) throws Exception {
		if (sourceValue instanceof Issue) {
			return _convert((Issue) sourceValue);
		} else if (sourceValue instanceof List) {
			return _convert((List<Issue>) sourceValue);
		}
		return "";
	}

	@Override
	public Map<ObjectType, Map<String, Object>> getMappingConstraints() {
		Map<ObjectType, Map<String, Object>> result = new HashMap<>();
		return result;
	}

	@Override
	public String remap(String sourceValue) throws Exception {
		// Return issue key as is
		return sourceValue;
	}

}
