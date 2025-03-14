package com.igsl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * VeraCode wants log data to be sanitized to prevent user data from masquerading as log data.
 * This is done by removing newline characters.
 */
public class Log {
	
	private static String processArguments(String format, Object... args) {
		List<Object> newArgs = new ArrayList<>();
		for (Object o : args) {
			if (o != null && o instanceof String) {
				String s = (String) o;
				newArgs.add(s.replaceAll("[\\r\\n]", ""));
			} else {
				newArgs.add(o);
			}
		}
		return String.format(format, newArgs);
	}
	
	public static void printCount(Logger logger, String title, int count, int total) {
		logger.info(title + count + "/" + total);
	}
	
	public static void error(Logger logger, String format, Object... args) {
		logger.error(processArguments(format, args));
	}

	public static void error(Logger logger, String format, Throwable ex) {
		logger.error(format, ex);
	}
	
	public static void debug(Logger logger, String format, Object... args) {
		logger.debug(processArguments(format, args));
	}

	public static void info(Logger logger, String format, Object... args) {
		logger.info(processArguments(format, args));
	}
	
	public static void warn(Logger logger, String format, Object... args) {
		logger.warn(processArguments(format, args));
	}
	
	public static void trace(Logger logger, String format, Object... args) {
		logger.trace(processArguments(format, args));
	}

	public static void fatal(Logger logger, String format, Object... args) {
		logger.fatal(processArguments(format, args));
	}
}
