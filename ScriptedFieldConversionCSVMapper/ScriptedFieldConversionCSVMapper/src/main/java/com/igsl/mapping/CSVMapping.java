package com.igsl.mapping;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.igsl.Console;
import com.igsl.Log;
import com.igsl.ScriptedFieldConversionConstants;
import com.igsl.dataconversion.DataConversion;
import com.igsl.dataconversion.DataConversion.ObjectType;
import com.igsl.dataconversion.DataConversionType;
import com.igsl.model.Issue;
import com.igsl.model.IssueType;
import com.igsl.model.Project;
import com.igsl.rest.ClientPool;
import com.igsl.rest.Paged;
import com.igsl.rest.RestUtil;
import com.igsl.rest.SinglePage;
import com.igsl.rest.Tokenized;

/**
 * Main class to perform data mapping in exported CSV
 */
public class CSVMapping {
	private static final Logger LOGGER = LogManager.getLogger(CSVMapping.class);
	private static final ObjectMapper OM = new ObjectMapper()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
	
	private static boolean loadMapping(
			Map<ObjectType, Map<String, String>> output, 
			String host, String email, String token, 
			ObjectType objectType, 
			Map<String, Object> constraints) {
		switch (objectType) {
		case ISSUE:
			try {
				Map<String, String> map = new HashMap<>();
				RestUtil<Issue> restUtil = RestUtil.getInstance(Issue.class)
						.host(host)
						.authenticate(email, token)
						.path("/rest/api/3/search/jql")
						.pagination(new Tokenized<Issue>(Issue.class))
						.method(HttpMethod.GET)
						.query(constraints);
				List<Issue> issues = restUtil.requestAllPages();
				for (Issue is : issues) {
					map.put(is.getKey(), is.getId());
				}
				output.put(ObjectType.ISSUE_TYPE, map);
			} catch (Exception ex) {
				Log.error(LOGGER, "Failed to retrieve issue mappings", ex);
				return false;
			}
			break;
		case ISSUE_TYPE:
			try {
				Map<String, String> map = new HashMap<>();
				RestUtil<IssueType> restUtil = RestUtil.getInstance(IssueType.class)
						.host(host)
						.authenticate(email, token)
						.path("/rest/api/3/issuetype")
						.pagination(new SinglePage<IssueType>(IssueType.class))
						.method(HttpMethod.GET);
				// This REST API accepts 0 parameters
				List<IssueType> issueTypes = restUtil.requestAllPages();
				for (IssueType it : issueTypes) {
					map.put(it.getName(), it.getId());
				}
				output.put(ObjectType.ISSUE_TYPE, map);
			} catch (Exception ex) {
				Log.error(LOGGER, "Failed to retrieve issue type mappings", ex);
				return false;
			}
			break;
		case PROJECT:
			try {
				Map<String, String> map = new HashMap<>();
				RestUtil<Project> restUtil = RestUtil.getInstance(Project.class);
				List<Project> projects = restUtil
						.host(host)
						.authenticate(email, token)
						.path("/rest/api/3/project/search")
						.pagination(new Paged<Project>(Project.class))
						.requestAllPages();
				// We always retreive all projects
				for (Project p : projects) {
					map.put(p.getKey(), p.getId());
				}
				output.put(ObjectType.PROJECT, map);
			} catch (Exception ex) {
				Log.error(LOGGER, "Failed to retrieve project mappings", ex);
				return false;
			}
			break;
		default:
			break;
		}
		return true;
	}
	
	public static void main(String[] args) {
		CommandLine cli = CLI.parse(args);
		if (cli != null) {
			String csvFile = cli.getOptionValue(CLI.OPTION_CSV);			
			String host = cli.getOptionValue(CLI.OPTION_HOST);
			String email = cli.getOptionValue(CLI.OPTION_EMAIL);
			String token = cli.getOptionValue(CLI.OPTION_API_TOKEN);
			if (token == null || token.isEmpty()) {
				// Read API token
				try {
					token = new String(Console.readPassword("Enter API token"));
				} catch (IOException ex) {
					Log.error(LOGGER, "Unable to read API token", ex);
				}
			}
			// Setup RestUtil client pool
			ClientPool.setMaxPoolSize(10, 0, 0);			
			// Initialize global mapping
			DataConversion.setGlobalMappings(new HashMap<>());
			for (ObjectType ot : ObjectType.values()) {
				if (ot.isGlobal()) {
					loadMapping(DataConversion.getGlobalMappings(), host, email, token, ot, null);
				}
			}
			// Parse CSV file
			CSVFormat writeFormat = CSVFormat.Builder.create()
					.setHeader(ScriptedFieldConversionConstants.CSV_HEADERS)
					.build();
			CSVFormat readFormat = CSVFormat.Builder.create()
					.setHeader(ScriptedFieldConversionConstants.CSV_HEADERS)
					.setSkipHeaderRecord(true)
					.build();
			Path csvPath = Paths.get(csvFile);
			Path outPath = csvPath.getParent().resolve("Remapped_" + csvPath.getFileName());
			try (	FileWriter fw = new FileWriter(outPath.toFile());
					CSVPrinter printer = new CSVPrinter(fw, writeFormat); 
					FileReader fr = new FileReader(csvPath.toFile());
					CSVParser parser = new CSVParser(fr, readFormat)) {
				Iterator<CSVRecord> it = parser.iterator();
				while (it.hasNext()) {
					CSVRecord record = it.next();
					List<String> valueList = record.toList();
					String dataConversion = record.get(
							ScriptedFieldConversionConstants.CSV_HEADER_DATA_CONVERSION);
					String value = record.get(
							ScriptedFieldConversionConstants.CSV_HEADER_CONVERTED_VALUE);
					DataConversionType dvType = DataConversionType.parse(dataConversion);
					if (dvType.getImplementation() != null) {
						// Initialize local mapping
						if (dvType.getImplementation().getLocalMappings() == null) {
							dvType.getImplementation().setLocalMappings(new HashMap<>());
							Map<ObjectType, Map<String, Object>> constraints = 
									dvType.getImplementation().getMappingConstraints();
							for (Map.Entry<ObjectType, Map<String, Object>> entry : constraints.entrySet()) {
								loadMapping(
										dvType.getImplementation().getLocalMappings(), 
										host, email, token, 
										entry.getKey(), entry.getValue());
							}
						}
						// Perform mapping
						String newValue = dvType.getImplementation().remap(value);
						valueList.set(ScriptedFieldConversionConstants.CSV_COLUMN_INDEX_CONVERTED_VALUE, 
								newValue);
					}
					// Write output CSV
					printer.printRecord((Object[]) valueList.toArray(new String[0]));
				}
			} catch (Exception ex) {
				Log.error(LOGGER, "Failed to read CSV file", ex);
			}
		}
	}
	
}
