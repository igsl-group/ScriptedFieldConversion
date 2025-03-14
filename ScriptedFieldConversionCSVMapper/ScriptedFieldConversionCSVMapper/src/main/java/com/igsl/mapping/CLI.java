package com.igsl.mapping;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CLI {
	
	public static Option OPTION_HOST = Option.builder()
			.desc("Jira Cloud host, e.g. https://kcwong.atlassian.net")
			.option("h")
			.longOpt("host")
			.hasArg()
			.required()
			.build();
	
	public static Option OPTION_EMAIL = Option.builder()
			.desc("User email")
			.option("u")
			.longOpt("user")
			.hasArg()
			.required()
			.build();
	
	public static Option OPTION_API_TOKEN = Option.builder()
			.desc("API token. Will prompt for input if not specified")
			.option("p")
			.longOpt("pass")
			.hasArg()
			.build();
	
	public static Option OPTION_CSV = Option.builder()
			.desc("CSV file to process")
			.option("f")
			.longOpt("file")
			.hasArg()
			.required()
			.build();
	
	public static Options OPTIONS_MAIN = new Options()
			.addOption(OPTION_API_TOKEN)
			.addOption(OPTION_CSV)
			.addOption(OPTION_EMAIL)
			.addOption(OPTION_HOST);
	
	public static CommandLine parse(String[] args) {
		CommandLineParser parser = new DefaultParser();
		try {
			return parser.parse(OPTIONS_MAIN, args);
		} catch (ParseException e) {
			// Print help
			HelpFormatter hf = new HelpFormatter();
			hf.printHelp("java -jar ScriptedFieldConversion-CSVMapping-[version].jar", OPTIONS_MAIN, true);
		}
		return null;
	}
}
