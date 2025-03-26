package com.igsl.search;

import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.igsl.Log;

public class IssueSpliterator implements Spliterator<List<Issue>> {

	private static final Logger LOGGER = Logger.getLogger(IssueSpliterator.class);
	private static final int DEFAULT_PAGE_SIZE = 1000;
	private static final JqlQueryParser JQL_PARSER = 
			ComponentAccessor.getComponent(JqlQueryParser.class);
	private static final SearchService SEARCH_SERVICE = 
			ComponentAccessor.getComponent(SearchService.class);
	
	private ApplicationUser user;
	private String jql;
	
	private int start;
	private int pageSize;
	private Query query;

	public IssueSpliterator(
			ApplicationUser user, 
			String jql) 
			throws JqlParseException {
		this.user = user;
		this.jql = jql;
		this.start = 0;
		this.pageSize = DEFAULT_PAGE_SIZE;
		this.query = JQL_PARSER.parseQuery(jql);
	}
	
	@Override
	public int characteristics() {
		// Paged results, so must be ordered
		return Spliterator.ORDERED;
	}

	@Override
	public long estimateSize() {
		return Long.MAX_VALUE;
	}

	@Override
	public boolean tryAdvance(Consumer<? super List<Issue>> consumer) {
		// Get next page
		try {
			SearchResults<Issue> result = SEARCH_SERVICE.search(
					user, 
					query, 
					PagerFilter.newPageAlignedFilter(start, pageSize));
			if (result.getResults().size() == 0) {
				// No more results
				return false;
			}
			start = result.getNextStart();
			List<Issue> list = result.getResults();
			consumer.accept(list);
			return true;
		} catch (SearchException e) {
			Log.error(LOGGER, "Error performing issue search", e);
			return false;
		}
	}

	@Override
	public Spliterator<List<Issue>> trySplit() {
		// Do not support parallel, return null
		return null;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

}
