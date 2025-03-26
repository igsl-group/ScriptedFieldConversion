package com.igsl.dataconversion;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.atlassian.jira.issue.Issue;

/**
 * Data conversion for VMNQ/VMNQCL projects.
 * 
 * In Jira server, the data is a HTML table.
 * In Jira cloud, the data is Markdown.
 * 
 * Switched from using DocumentBuilder to JSoup due to the data not being proper XHTML.
 */
public class QuoteResponses extends DataConversion {

	public static final String NAME = "Quote Response/s";
	public static final String DESC = "HTML table => Markdown";
	
	private static final Pattern PATTERN_ISSUE_URL = Pattern.compile(".*/browse/(.+)");
	
	private static final String SUBTASK_NAME = "Quote Response";
	
//	private static final String HTML_BEGIN = "<html><body>";
//	private static final String HTML_END = "</body></html>";
	
	private static final String MARKDOWN_TABLE_HEADER = "||*Summary*||*Status*||";
	private static final String NO_QUOTE_RESPONSE = "There are no Quote Response(s) for this issue";	
	private static final String NEWLINE = "\r\n";
	
	@Override
	public Object convert(
			Issue issue,
			Object sourceValue) throws Exception {
		StringBuilder output = new StringBuilder();
		Document doc = Jsoup.parse(String.valueOf(sourceValue));
		Elements linkList = doc.getElementsByTag("a");
		linkList.forEach(element -> {
			StringBuilder row = new StringBuilder();
			String linkURL = element.attr("href");
			String newUrl = "";
			Matcher matcher = PATTERN_ISSUE_URL.matcher(linkURL);
			if (matcher.matches()) {
				newUrl = "/browse/" + matcher.group(1);
			}
			String linkText = element.text();
			Elements spanList = element.parent().parent().getElementsByTag("span");
			String status = spanList.get(0).text();
			row	.append(NEWLINE)
				.append("|[")
				.append(linkText)
				.append("|")
				.append(newUrl)
				.append("]|");
			// Get status text from under parent tr
			if (status != null) {
				row.append(status).append("|");
			} else {
				row.append("--|");
			}
			output.append(row);
		});
		if (output.length() == 0) {
			output.insert(0, NO_QUOTE_RESPONSE + NEWLINE);
		} else {
			output.insert(0, MARKDOWN_TABLE_HEADER);
			output.append(NEWLINE);	// End of table line break
		}
		// Create subtask URL syntax
		// https://kcwong.atlassian.net/jira/secure/CreateSubTaskIssue.jspa?pid=10044&issuetype=10006&parentIssueId=10640
		// The IDs remain server ids as it is impossible to map pre-JCMA
		// The output CSV has to be mapped before import
		output	.append("[Add new Quote Response|")
				.append("/jira/secure/CreateSubTaskIssue.jspa?")
				.append("pkey=").append(issue.getProjectObject().getKey())
				.append("&issuetypeName=").append(SUBTASK_NAME)
				.append("&parentIssueKey=").append(issue.getKey())
				.append("]");
		return output.toString();
	}
	
	/*
	@Override
	public Object convert(
			Issue issue, 
			Object sourceValue) throws Exception {

//		Sample data: 
//		<table class='aui' style='margin-bottom: 8px;'>
//			<thead>
//				<tr>
//					<th style='white-space: nowrap;'>Summary</th>
//					<th style='white-space: nowrap;'>Status</th>
//				</tr>
//			</thead>
//			<tbody class='ui-sortable'>
//				<tr class='issuerow' style='display: table-row;'>
//					<td style='word-break: normal;'>
//						<a href='/browse/VMNQ-2006'>Xenith IG | Dark Fiber | SIN03 | | 25 Serango | | Terrestrial</a>
//					</td>
//					<td style='word-break: normal;'> 
//						<span class='jira-issue-status-lozenge aui-lozenge jira-issue-status-lozenge-new aui-lozenge-subtle jira-issue-status-lozenge-max-width-short'>Ticket opened</span>
//					</td>
//				</tr>
//			</tbody>
//		</table>
//		<button class='aui-button s-editor-dialog' style='margin-bottom: 12px;' onclick='JIRA.Forms.createSubtaskForm({parentIssueId: 645677}).asDialog({windowTitle:"Create Quote Response", id: "create-subtask-dialog"}).show();'>
//			<span class='icon aui-icon aui-icon-small aui-iconfont-add'></span> 
//			<span class='trigger-label'>Add new Quote Response</span>
//		</button>
//		<br />
//		Target format:
//			||*Summary*||*Status*||
//			|[Text|URL]|Text|
//			[Add new Quote Resposne|URL to create]

		// Make data valid XHTML so it can be parsed by DocumentBuilder
		// TODO Possibly switch to use https://jsoup.org/ for better parsing
		sourceValue = HTML_BEGIN + sourceValue + HTML_END;
		StringBuilder output = new StringBuilder();
		output.append(MARKDOWN_TABLE_HEADER);
		// Use XPath to grab issue links out of the table
		XPath xPath = XPathFactory.newInstance().newXPath();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new StringReader(String.valueOf(sourceValue))));
		// Get links
		NodeList linkList = (NodeList) xPath.evaluate("//a", doc, XPathConstants.NODESET);
		for (int idx = 0; idx < linkList.getLength(); idx++) {
			StringBuilder row = new StringBuilder();
			Node linkNode = linkList.item(idx);
			String url = linkNode.getAttributes().getNamedItem("href").getNodeValue();	
			String newUrl = "";
			Matcher matcher = PATTERN_ISSUE_URL.matcher(url);
			if (matcher.matches()) {
				newUrl = "/browse/" + matcher.group(1);
			}
			String linkText = linkNode.getTextContent();
			row	.append(NEWLINE)
				.append("|[")
				.append(linkText)
				.append("|")
				.append(newUrl)
				.append("]|");
			// Get status text from under parent tr
			Node statusNode = (Node) xPath.evaluate("./ancestor::tr/td/span", linkNode, XPathConstants.NODE);
			if (statusNode != null) {
				row.append(statusNode.getTextContent()).append("|");
			} else {
				row.append("--|");
			}
			output.append(row);
		}	// For each link found
		// Create subtask URL syntax
		// https://kcwong.atlassian.net/jira/secure/CreateSubTaskIssue.jspa?pid=10044&issuetype=10006&parentIssueId=10640
		// The IDs remain server ids as it is impossible to map pre-JCMA
		// The output CSV has to be mapped before import
		output	.append(NEWLINE)
				.append("[Add new Quote Response|")
				.append("/jira/secure/CreateSubTaskIssue.jspa?")
				.append("pkey=").append(issue.getProjectObject().getKey())
				.append("&issuetypeName=").append(SUBTASK_NAME)
				.append("&parentIssueKey=").append(issue.getKey())
				.append("]");
		return output.toString();
	}
	*/

	@Override
	public Map<ObjectType, Map<String, Object>> getMappingConstraints() {
		Map<ObjectType, Map<String, Object>> result = new HashMap<>();
		Map<String, Object> map = new HashMap<>();
		map.put("jql", "project IN (VMNQ, VMNQCL)");
		result.put(ObjectType.ISSUE, map);
		return result;
	}

	private static final Pattern ADD_SUBTASK_PATTERN = Pattern.compile(
			"^(.+?)\\[Add new Quote Response\\|\\/jira\\/secure\\/CreateSubTaskIssue\\.jspa\\?pkey=(.+?)&issuetypeName=(.+?)&parentIssueKey=(.+?)\\]$",
			Pattern.DOTALL);
	
	@Override
	public String remap(String sourceValue) throws Exception {
		StringBuilder result = new StringBuilder("");
		Matcher matcher = ADD_SUBTASK_PATTERN.matcher(sourceValue);
		if (matcher.matches()) {
			StringBuilder errorMessage = new StringBuilder();
			String table = matcher.group(1);
			String projectKey = matcher.group(2);
			String issueTypeName = matcher.group(3);
			String parentIssueKey = matcher.group(4);
			String projectId = 
					globalMappings.containsKey(ObjectType.PROJECT)? 
							globalMappings.get(ObjectType.PROJECT).get(projectKey) : 
							null;
			if (projectId == null) {
				errorMessage.append("Project key [").append(projectKey).append("]").append(NEWLINE);
			}
			String issueTypeId = 
					globalMappings.containsKey(ObjectType.ISSUE_TYPE)? 
							globalMappings.get(ObjectType.ISSUE_TYPE).get(issueTypeName): 
							null;
			if (issueTypeId == null) {
				errorMessage.append("Issue type name [").append(issueTypeName).append("]").append(NEWLINE);
			}
			String parentIssueId = 
					localMappings.containsKey(ObjectType.ISSUE)? 
							localMappings.get(ObjectType.ISSUE).get(parentIssueKey): 
							null;
			if (parentIssueId == null) {
				errorMessage.append("Parent issue key [").append(parentIssueKey).append("]").append(NEWLINE);
			}			
			if (projectId == null || 
				issueTypeId == null || 
				parentIssueId == null) {
				throw new Exception("Unable to map: " + errorMessage.toString());
			}
			result	.append(table)
					.append(NEWLINE)
					.append("[Add new Quote Response|")
					.append("/jira/secure/CreateSubTaskIssue.jspa?")
					.append("pid=").append(projectId)
					.append("&issuetype=").append(issueTypeId)
					.append("&parentIssueId=").append(parentIssueId)
					.append("]");
		} else {
			throw new Exception("Unable to map: regex not match");
		}
		return result.toString();
	}

}
