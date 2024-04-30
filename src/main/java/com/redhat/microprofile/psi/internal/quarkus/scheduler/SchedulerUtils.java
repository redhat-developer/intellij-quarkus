/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.scheduler;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for quarkus scheduler
 *
 */
public class SchedulerUtils {

	public static enum ValidationType {
		cron, duration
	}

	// Patterns for each cron part of the cron member expression in order of
	// SchedulerErrorCode enum ordinal
	// Note: DAY_OF_MONTH and DAY_OF_WEEK are mutually exclusive

	private static final String SECOND_MINUTE_RANGE = "([1-5]?[0-9])";

	private static final String HOUR_RANGE = "(2[0-3]|1?[0-9])";

	private static final String DAY_OF_MONTH_RANGE = "(3[01]|[12][0-9]|[1-9])";

	private static final String MONTH_RANGE = "(JAN|FEB|MA[RY]|APR|JU[NL]|AUG|SEP|OCT|NOV|DEC|1[0-2]|[1-9])";

	private static final String DAY_OF_WEEK_RANGE = "(MON|TUE|WED|THU|FRI|SAT|SUN|[1-7])";

	private static final String YEAR_RANGE = "(19[7-9][0-9]|20[0-9][0-9])";

	private static final String CRON_SPECIAL_CHARACTERS = "|\\*|\\?|^\\*\\/\\d+";

	private static final String BASE_CRON_PATTERN = "(%1$s(?:,%1$s)*|%1$s\\-%1$s|%2$s)";

	private static final Pattern[] CRON_PATTERNS = {
			Pattern.compile(String.format(BASE_CRON_PATTERN, SECOND_MINUTE_RANGE, CRON_SPECIAL_CHARACTERS)), // SECONDS
			Pattern.compile(String.format(BASE_CRON_PATTERN, SECOND_MINUTE_RANGE, CRON_SPECIAL_CHARACTERS)), // MINUTES
			Pattern.compile(String.format(BASE_CRON_PATTERN, HOUR_RANGE, CRON_SPECIAL_CHARACTERS)), // HOUR
			Pattern.compile(String.format(BASE_CRON_PATTERN, DAY_OF_MONTH_RANGE, CRON_SPECIAL_CHARACTERS)), // DAY_OF_MONTH
			Pattern.compile(String.format(BASE_CRON_PATTERN, MONTH_RANGE, CRON_SPECIAL_CHARACTERS),
					Pattern.CASE_INSENSITIVE), // MONTH
			Pattern.compile(String.format(BASE_CRON_PATTERN, DAY_OF_WEEK_RANGE, CRON_SPECIAL_CHARACTERS),
					Pattern.CASE_INSENSITIVE), // DAY_OF_WEEK
			Pattern.compile(String.format(BASE_CRON_PATTERN, YEAR_RANGE, CRON_SPECIAL_CHARACTERS)) // YEAR
	};

	private static final Pattern TIME_PERIOD_PATTERN = Pattern.compile("\\d+[smhSMH]$");

	// Define the regex pattern for environment variables with optional default values (allowing spaces in default values) and optional prefix
	private static final Pattern ENV_PATTERN = Pattern.compile("^\\$?\\{([^\\{\\}:\\s=]+)(?:\\:([^\\{\\}=]*))?\\}$");

	private static final Pattern LOOSE_ENV_PATTERN = Pattern.compile("^.*\\$?\\{.*\\}.*$");

	private SchedulerUtils() {
	}

	/**
	 * Validate the @Scheduled cron member with each cron string part and return an
	 * error message if necessary
	 *
	 * @param cronString the cron member value
	 * @return the error fault for the cron string validation and <code>null</code> if valid
	 */
	public static SchedulerErrorCodes validateCronPattern(String cronString) {

		String[] cronParts = cronString.split("\\s+");

		if (cronParts.length < 6 || cronParts.length > 7) {
			return SchedulerErrorCodes.INVALID_CRON_LENGTH;
		}

		SchedulerErrorCodes[] errorCodes = SchedulerErrorCodes.values();
		for (int i = 0; i < cronParts.length; i++) {
			if (!CRON_PATTERNS[i].matcher(cronParts[i]).matches()) {
				return errorCodes[i];
			}
		}
		return null;
	}

	/**
	 * Validate the string from the @Scheduled member can be parsed to a Duration
	 * unit
	 *
	 * @param timePattern the member value for the time pattern
	 * @return the INVALID_DURATION_PARSE_PATTERN error code if invalid and <code>null</code> if
	 *         valid
	 */
//	\\d+[smhSMH]
	public static SchedulerErrorCodes validateDurationParse(String timePattern) {
		if (!TIME_PERIOD_PATTERN.matcher(timePattern).matches()) {
			try {
				Duration.parse(timePattern);
			} catch (DateTimeParseException e) {
				return SchedulerErrorCodes.INVALID_DURATION_PARSE_PATTERN;
			}
		}
		return null;
	}

	/**
	 * Check if the member value env variable pattern is well formed
	 *
	 * @param memberValue the member value from expression
	 * @param validationType the type of validation to perform on the default value
	 * @return a SchedulerErrorCodes if memberValue is invalid,
	 * <code>null</code> otherwise.
	 */
	public static SchedulerErrorCodes matchEnvMember(String memberValue, ValidationType validationType) {
		return checkLooseEnvPattern(memberValue) ? checkEnvPattern(memberValue, validationType) : null;
	}

	/**
	 * Match the member to an env variable pattern
	 *
	 * @param memberValue the member value from expression
	 * @param validationType the type of validation to perform on the default value
	 * @return SchedulerErrorCodes if the variable is invalid or the default value is not a valid cron expression,
	 * <code>SchedulerErrorCodes.VALID_EXPRESSION</code> otherwise.
	 */
	private static SchedulerErrorCodes checkEnvPattern(String memberValue, ValidationType validationType) {
		Matcher matcher = ENV_PATTERN.matcher(memberValue);
		if (matcher.matches()) {
			String defaultValue = matcher.group(2);
			if (defaultValue == null) {
				return SchedulerErrorCodes.VALID_EXPRESSION;
			}
			SchedulerErrorCodes defaultValueValidation;
			if (validationType == ValidationType.cron) {
				defaultValueValidation = validateCronPattern(defaultValue);
			} else {
				defaultValueValidation = validateDurationParse(defaultValue);
			}
			return defaultValueValidation == null? SchedulerErrorCodes.VALID_EXPRESSION: defaultValueValidation;
		}
		return SchedulerErrorCodes.INVALID_CHAR_IN_EXPRESSION;
	}

	/**
	 * Match the member to a loose env variable pattern
	 *
	 * @param memberValue the member value from expression
	 * @return true if pattern is loose env variable, false otherwise
	 */
	private static boolean checkLooseEnvPattern(String memberValue) {
		return LOOSE_ENV_PATTERN.matcher(memberValue).matches();
	}
}
