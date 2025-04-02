package com.igsl.mapping;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CLI {
	
	public static class Command {
		private CommandType type;
		private CommandLine cmd;
		public Command(CommandType type, CommandLine cmd) {
			this.type = type;
			this.cmd = cmd;
		}
		public CommandType getType() {
			return type;
		}
		public void setType(CommandType type) {
			this.type = type;
		}
		public CommandLine getCmd() {
			return cmd;
		}
		public void setCmd(CommandLine cmd) {
			this.cmd = cmd;
		}
	}
	
	public static enum CommandType {
		MAP_CSV(CLI.OPTIONS_REMAP),
		IMPORT_DATA(CLI.OPTIONS_IMPORT);
		private Options options;
		private CommandType(Options options) {
			this.options = options;
		}
		public Options getOptions() {
			return this.options;
		}
	}
	
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
	
	public static Option OPTION_CONFIG = Option.builder()
			.desc("Import configuration file to process")
			.option("c")
			.longOpt("config")
			.hasArg()
			.required()
			.build();
	
	public static Option OPTION_REMAP = Option.builder()
			.desc("Remap CSV")
			.option("m")
			.longOpt("remap")
			.required()
			.build();
	public static Options OPTIONS_REMAP = new Options()
			.addOption(OPTION_API_TOKEN)
			.addOption(OPTION_CSV)
			.addOption(OPTION_CONFIG)
			.addOption(OPTION_EMAIL)
			.addOption(OPTION_HOST)
			.addOption(OPTION_REMAP);
	
	public static Option OPTION_TARGET_FIELD = Option.builder()
			.desc("Field ID to import Converted Value into, e.g. customfield_12345")
			.option("t")
			.longOpt("targetField")
			.hasArg()
			.required()
			.build();
	public static Option OPTION_IMPORT = Option.builder()
			.desc("Import CSV")
			.option("i")
			.longOpt("import")
			.required()			
			.build();
	public static Options OPTIONS_IMPORT = new Options() 
			.addOption(OPTION_API_TOKEN)
			.addOption(OPTION_CSV)
			.addOption(OPTION_EMAIL)
			.addOption(OPTION_HOST)
			.addOption(OPTION_IMPORT)
			.addOption(OPTION_TARGET_FIELD);
			
	public static Command parse(String[] args) {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		CommandType type = null;
		for (CLI.CommandType c : CLI.CommandType.values()) {
			try {
				cmd = parser.parse(c.getOptions(), args);
				type = c;
			} catch (ParseException e) {
				// Ignore error
			}
		}
		if (cmd != null && type != null) {
			return new Command(type, cmd);
		}
		// Print help
		HelpFormatter hf = new HelpFormatter();
		hf.printHelp("java -jar ScriptedFieldConversion-CSVMapping-[version].jar", OPTIONS_REMAP, true);
		hf.printHelp("java -jar ScriptedFieldConversion-CSVMapping-[version].jar", OPTIONS_IMPORT, true);
		return null;
	}
}
