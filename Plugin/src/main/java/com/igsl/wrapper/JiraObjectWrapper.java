package com.igsl.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.atlassian.jira.issue.Issue;

/**
 * Wrapper for Jira objects to avoid circular references when serializing with Jackson
 * We can't add annotation to Jira object classes
 * So a wrapper is the only way
 */
public class JiraObjectWrapper {
	public static Object wrap(Object o) {
		if (o instanceof Issue) {
			// Issue
			return new IssueWrapper((Issue) o);
		} else if (o instanceof Collection) {
			// List of Issue
			List<Object> list = new ArrayList<>();
			for (Object item : (Collection) o) {
				list.add(wrap(item));
			}
			return list;
		}
		return o;
	}
}
