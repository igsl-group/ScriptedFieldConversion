package com.igsl.mapping;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.igsl.Console;
import com.igsl.Log;
import com.igsl.ScriptedFieldConversionConstants;
import com.igsl.dataconversion.DataConversion;
import com.igsl.dataconversion.DataConversion.ObjectType;
import com.igsl.importconfig.ImportConfig;
import com.igsl.mapping.CLI.Command;
import com.igsl.dataconversion.DataConversionType;
import com.igsl.model.CustomField;
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
	
	private static final String REMAPPED = "Remapped_";
	
	// TODO Implement a way to override constraints from DataConversion implementation?
	// So I can test stuff
	
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
		case CUSTOM_FIELD: 
			try {
				Map<String, String> map = new HashMap<>();
				RestUtil<CustomField> restUtil = RestUtil.getInstance(CustomField.class)
						.host(host)
						.authenticate(email, token)
						.path("/rest/api/3/field")
						.pagination(new SinglePage<CustomField>(CustomField.class))
						.method(HttpMethod.GET);
				// This REST API accepts 0 parameters
				List<CustomField> fields = restUtil.requestAllPages();
				for (CustomField field : fields) {
					if (map.containsKey(field.getName())) {
						map.put(field.getName(), 
								map.get(field.getName()) + "|" + field.getId());
					} else {
						map.put(field.getName(), field.getId());
					}
				}
				output.put(ObjectType.CUSTOM_FIELD, map);
			} catch (Exception ex) {
				Log.error(LOGGER, "Failed to retrieve custom field mappings", ex);
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

	private static void importData(Command cli) {
		final String csvFile = cli.getCmd().getOptionValue(CLI.OPTION_CSV);			
		final String host = cli.getCmd().getOptionValue(CLI.OPTION_HOST);
		final String email = cli.getCmd().getOptionValue(CLI.OPTION_EMAIL);
		String token = cli.getCmd().getOptionValue(CLI.OPTION_API_TOKEN);
		if (token == null || token.isEmpty()) {
			// Read API token
			try {
				token = new String(Console.readPassword("Enter API token"));
			} catch (IOException ex) {
				Log.error(LOGGER, "Unable to read API token", ex);
			}
		}
		final String finalToken = token;
		final String targetField = cli.getCmd().getOptionValue(CLI.OPTION_TARGET_FIELD);
		// Setup RestUtil client pool
		ClientPool.setMaxPoolSize(10, 0, 0);
		// Parse CSV
		CSVFormat readFormat = CSVFormat.Builder.create()
				.setHeader(ScriptedFieldConversionConstants.CSV_HEADERS.toArray(new String[0]))
				.setSkipHeaderRecord(true)
				.build();
		Path csvPath = Paths.get(csvFile);
		try (	FileReader fr = new FileReader(csvPath.toFile()); 
				CSVParser csvParser = new CSVParser(fr, readFormat)) {
			csvParser.forEach(csvRecord -> {
				String issueKey = csvRecord.get(
						ScriptedFieldConversionConstants.CSV_COLUMN_INDEX_ISSUE_KEY);
				String convertedValue = csvRecord.get(
						ScriptedFieldConversionConstants.CSV_COLUMN_INDEX_CONVERTED_VALUE);
				Log.info(LOGGER, "Processing: " + issueKey + " Value: " + convertedValue);
				// Import data
				Map<String, Object> fieldMap = new HashMap<>();
				fieldMap.put(targetField, convertedValue);	
				Map<String, Object> payload = new HashMap<>();
				payload.put("fields", fieldMap);
				try {
					Response resp = RestUtil.getInstance(Object.class)
						.host(host)
						.authenticate(email, finalToken)
						.method(HttpMethod.PUT)
						.path("/rest/api/2/issue/{issueKey}")
						.pathTemplate("issueKey", issueKey)
						.payload(payload)
						.status(null)
						.request();
					Log.info(LOGGER, issueKey + ": " + resp.getStatus());
					Log.info(LOGGER, "Resp: " + resp.readEntity(String.class)); 
				} catch (Exception ex) {
					Log.error(LOGGER, "Error updating issue " + issueKey, ex);
				}
			});
		} catch (IOException ioex) {
			Log.error(LOGGER, "Failed to read CSV file", ioex);
		} 
	}
	
	private static void remapCSV(Command cli) {
		String csvFile = cli.getCmd().getOptionValue(CLI.OPTION_CSV);			
		String configFile = cli.getCmd().getOptionValue(CLI.OPTION_CONFIG);
		String host = cli.getCmd().getOptionValue(CLI.OPTION_HOST);
		String email = cli.getCmd().getOptionValue(CLI.OPTION_EMAIL);
		String token = cli.getCmd().getOptionValue(CLI.OPTION_API_TOKEN);
		if (token == null || token.isEmpty()) {
			// Read API token
			try {
				token = new String(Console.readPassword("Enter API token"));
			} catch (IOException ex) {
				Log.error(LOGGER, "Unable to read API token", ex);
			}
		}
		Log.info(LOGGER, "Retrieving object IDs from Jira Cloud");
		// Setup RestUtil client pool
		ClientPool.setMaxPoolSize(10, 0, 0);			
		// Initialize global mapping
		DataConversion.setGlobalMappings(new HashMap<>());
		for (ObjectType ot : ObjectType.values()) {
			if (ot.isGlobal()) {
				loadMapping(DataConversion.getGlobalMappings(), host, email, token, ot, null);
			}
		}
		try {
			Log.debug(LOGGER, "Global mappings: " + 
					OM.writeValueAsString(DataConversion.getGlobalMappings()));
		} catch (JsonProcessingException e) {
			Log.error(LOGGER, "Global mappings: JSON error", e);
		}
		// Parse config file
		Log.info(LOGGER, "Processing config file: " + configFile);
		try {
			ObjectWriter writer = OM.writerFor(ImportConfig.class);
			ObjectReader reader = OM.readerFor(ImportConfig.class);
			Path configIn = Paths.get(configFile);
			ImportConfig config = reader.readValue(configIn.toFile());
			String fieldName = config.getConfigFieldMappings()
				.get(ScriptedFieldConversionConstants.CSV_HEADER_CONVERTED_VALUE)
				.get(ScriptedFieldConversionConstants.JIRA_FIELD);
			if (DataConversion.getGlobalMappings().get(ObjectType.CUSTOM_FIELD)
					.containsKey(fieldName)) {
				String fieldId = DataConversion.getGlobalMappings().get(ObjectType.CUSTOM_FIELD)
						.get(fieldName);
				config.getConfigFieldMappings()
					.get(ScriptedFieldConversionConstants.CSV_HEADER_CONVERTED_VALUE)
					.put(	ScriptedFieldConversionConstants.JIRA_FIELD, 
							fieldId);
				Path configOut = configIn.getParent().resolve(REMAPPED + configIn.getFileName());
				writer.writeValue(configOut.toFile(), config);
				Log.info(LOGGER, "Config file updated");
			} else {
				Log.info(LOGGER, "Custom field mapping cannot be found, config file unchanged");
			}
		} catch (Exception ex) {
			Log.error(LOGGER, "Error proxessing config file", ex);
		}
		Log.info(LOGGER, "Processing CSV file: " + csvFile);
		// Parse CSV file
		CSVFormat writeFormat = CSVFormat.Builder.create()
				.setHeader(ScriptedFieldConversionConstants.CSV_HEADERS.toArray(new String[0]))
				.build();
		CSVFormat readFormat = CSVFormat.Builder.create()
				.setHeader(ScriptedFieldConversionConstants.CSV_HEADERS.toArray(new String[0]))
				.setSkipHeaderRecord(true)
				.build();
		Path csvPath = Paths.get(csvFile);
		Path outPath = csvPath.getParent().resolve(REMAPPED + csvPath.getFileName());
		try (	FileWriter fw = new FileWriter(outPath.toFile());
				CSVPrinter printer = new CSVPrinter(fw, writeFormat); 
				FileReader fr = new FileReader(csvPath.toFile());
				CSVParser parser = new CSVParser(fr, readFormat)) {
			Iterator<CSVRecord> it = parser.iterator();
			while (it.hasNext()) {
				CSVRecord record = it.next();
				List<String> valueList = record.toList();
				String issueKey = record.get(
						ScriptedFieldConversionConstants.CSV_HEADER_ISSUE_KEY);
				String dataConversion = record.get(
						ScriptedFieldConversionConstants.CSV_HEADER_DATA_CONVERSION);
				String value = record.get(
						ScriptedFieldConversionConstants.CSV_HEADER_CONVERTED_VALUE);
				Log.info(LOGGER, "Process issue: " + issueKey);
				Log.debug(LOGGER, "Data Conversion used: " + dataConversion);
				Log.debug(LOGGER, "Original value: " + value);					
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
						try {
							Log.debug(LOGGER, "Local mappings: " + 
									OM.writeValueAsString(dvType.getImplementation().getLocalMappings()));
						} catch (JsonProcessingException e) {
							Log.error(LOGGER, "Local mappings: JSON error", e);
						}
					}
					// Perform mapping
					try {
						String newValue = dvType.getImplementation().remap(value);
						Log.debug(LOGGER, "Remapped value: " + newValue);
						valueList.set(ScriptedFieldConversionConstants.CSV_COLUMN_INDEX_CONVERTED_VALUE, 
								newValue);
					} catch (Exception ex) {
						Log.error(LOGGER, ex.getMessage());
					}
				}
				// Write output CSV
				printer.printRecord((Object[]) valueList.toArray(new String[0]));
			}
		} catch (Exception ex) {
			Log.error(LOGGER, "Failed to read CSV file", ex);
		}
	}
	
	public static void main(String[] args) {
		Command cli = CLI.parse(args);
		if (cli != null) {
			switch (cli.getType()) {
			case IMPORT_DATA:
				importData(cli);
				break;
			case MAP_CSV:
				remapCSV(cli);
				break;
			default:
				break;
			}
		}
	}
	
}
