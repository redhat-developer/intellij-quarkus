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

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaErrorCode;

/**
 * The error code and message for the @Scheduled cron string
 *
 * NOTE: The order of the enumeration matters for the proper warning message to
 * display. If any enumerations are added, please append it to the end of the
 * enumeration list.
 */
@SuppressWarnings("unchecked")
public enum SchedulerErrorCodes implements IJavaErrorCode {
	INVALID_CRON_SECOND("Seconds must be within the range 0-59."),
	INVALID_CRON_MINUTE("Minutes must be within the range 0-59."),
	INVALID_CRON_HOUR("Hour must be within the range 0-23."),
	INVALID_CRON_DAY_OF_MONTH("Day of month must be within the range 1-31."),
	INVALID_CRON_MONTH("Month must be within the range 1-12 or a supported 3 letter abbreviation."),
	INVALID_CRON_DAY_OF_WEEK("Day of week must be within range 1-7 or a supported 3 letter abbreviation."),
	INVALID_CRON_YEAR("Year must be in the range [1970, 2099]"),
	INVALID_CRON_LENGTH("The cron expression must contain 6-7 parts, delimited by whitespace."),
	INVALID_DURATION_PARSE_PATTERN("Text cannot be parsed to a Duration."),
	INVALID_CHAR_IN_EXPRESSION("Invalid char(s) in expression."),
	VALID_EXPRESSION("Expression is valid.");

	private final String errorMessage;

	SchedulerErrorCodes(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public String getCode() {
		return Integer.toString(ordinal());
	}

}