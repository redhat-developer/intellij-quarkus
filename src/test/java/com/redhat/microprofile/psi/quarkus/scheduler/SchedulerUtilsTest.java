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
package com.redhat.microprofile.psi.quarkus.scheduler;

import com.redhat.microprofile.psi.internal.quarkus.scheduler.SchedulerErrorCodes;
import com.redhat.microprofile.psi.internal.quarkus.scheduler.SchedulerUtils;
import org.junit.Assert;
import org.junit.Test;

public class SchedulerUtilsTest {

	@Test
	public void validCronPatternMinLengthTest() {
		String cronPattern = "* * * * * *";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertNull(actual);
	}

	@Test
	public void validCronPatternMaxLengthTest() {
		String cronPattern = "* * * * * * *";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertNull(actual);
	}

	@Test
	public void invalidCronPatternUnderLengthTest() {
		String cronPattern = "* * * * *";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertEquals(SchedulerErrorCodes.INVALID_CRON_LENGTH, actual);
	}

	@Test
	public void invalidCronPatternOverLengthTest() {
		String cronPattern = "* * * * * * * *";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertEquals(SchedulerErrorCodes.INVALID_CRON_LENGTH, actual);
	}

	@Test
	public void validCronPatternExclusionCharacterTest() {
		String cronPattern = "? ? ? ? ? ?";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertNull(actual);
	}

	@Test
	public void validCronPatternCommaSeperatedTest() {
		String cronPattern = "0,1,59 0,1,59 0,1,23 1,2,31 1,2,12 1,2,7 1970,1971,2099";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertNull(actual);
	}

	@Test
	public void validCronPatternCommaSeperatedAbbrevTest() {
		String cronPattern = "* * * * JAN,FEB,DEC MON,TUE,SUN";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertNull(actual);
	}

	@Test
	public void invalidCronPatternCommaSeperatedTest() {
		String cronPattern = "0,1,60 * * * * *";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertEquals(SchedulerErrorCodes.INVALID_CRON_SECOND, actual);
	}

	@Test
	public void validCronPatternRangeTest() {
		String cronPattern = "0-59 0-59 0-23 1-31 1-12 1-7 1970-2099";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertNull(actual);
	}

	@Test
	public void validCronPatternRangeAbbrevTest() {
		String cronPattern = "* * * * JAN-DEC MON-SUN";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertNull(actual);
	}

	@Test
	public void invalidCronPatternRangeTest() {
		String cronPattern = "0-60 * * * * *";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertEquals(SchedulerErrorCodes.INVALID_CRON_SECOND, actual);
	}

	@Test
	public void cronMultipleInvalidTest() {
		String cronPattern = "60 60 24 32 13 ? 1969";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertEquals(SchedulerErrorCodes.INVALID_CRON_SECOND, actual);
	}

	@Test
	public void cronFirstValidMultipleInvalidTest() {
		String cronPattern = "59 60 24 32 13 ? 1969";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertEquals(SchedulerErrorCodes.INVALID_CRON_MINUTE, actual);
	}

	@Test
	public void validCronCaseMixingMonthAbbrevTest() {
		String cronPattern = "* * * * jAn *";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertNull(actual);
	}

	@Test
	public void validCronCaseMixingDayOfWeekAbbrevTest() {
		String cronPattern = "* * * * * mOn";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertNull(actual);
	}

	@Test
	public void validCronIntervalTest() {
		String cronPattern = "*/5 * * * * *";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertNull(actual);
	}

	@Test
	public void invalidCronIntervalTest() {
		String cronPattern = "*/s * * * * *";
		SchedulerErrorCodes actual = SchedulerUtils.validateCronPattern(cronPattern);
		Assert.assertEquals(SchedulerErrorCodes.INVALID_CRON_SECOND, actual);
	}

	@Test
	public void validDurationIntervalTest() {
		String durationPattern = "5m";
		SchedulerErrorCodes actual = SchedulerUtils.validateDurationParse(durationPattern);
		Assert.assertNull(actual);
	}

	@Test
	public void validDurationISO8601Test() {
		String durationPattern = "PT15M";
		SchedulerErrorCodes actual = SchedulerUtils.validateDurationParse(durationPattern);
		Assert.assertNull(actual);
	}

	@Test
	public void valid$EnvMemberTest() {
		String envMember = "${some.expression}";
		SchedulerErrorCodes actual = SchedulerUtils.matchEnvMember(envMember);
		Assert.assertEquals(SchedulerErrorCodes.VALID_EXPRESSION, actual);
	}

	@Test
	public void validNo$EnvMemberTest() {
		String envMember = "{some.expression}";
		SchedulerErrorCodes actual = SchedulerUtils.matchEnvMember(envMember);
		Assert.assertEquals(SchedulerErrorCodes.VALID_EXPRESSION, actual);
	}

	@Test
	public void validEnvMemberTestWithParamTest() {
		String envMember = "${some.expression:off}";
		SchedulerErrorCodes actual = SchedulerUtils.matchEnvMember(envMember);
		Assert.assertEquals(SchedulerErrorCodes.VALID_EXPRESSION, actual);
	}

	@Test
	public void invalidEnvMemberTest() {
		String envMember = "$${some?expression}}";
		SchedulerErrorCodes actual = SchedulerUtils.matchEnvMember(envMember);
		Assert.assertEquals(SchedulerErrorCodes.INVALID_CHAR_IN_EXPRESSION, actual);
	}

	@Test
	public void notEnvMemberTest() {
		String envMember = "nocurlybraceorenvvar";
		SchedulerErrorCodes actual = SchedulerUtils.matchEnvMember(envMember);
		Assert.assertNull(actual);
	}
}