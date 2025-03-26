package com.igsl.search;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.user.ApplicationUser;

public class IssueSearchUtil {
	
	public static Stream<Issue> streamOf(
			ApplicationUser user,
			String jql) throws JqlParseException {
		return StreamSupport
					.stream(new IssueSpliterator(user, jql), false)
					.flatMap(list -> list.stream());
	}
}
