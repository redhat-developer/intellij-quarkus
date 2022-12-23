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

package org.eclipse.microprofile.fault.tolerance.tck.invalidParameters;

import java.sql.Connection;
import java.time.temporal.ChronoUnit;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import org.eclipse.microprofile.faulttolerance.Retry;

@Retry(delay = 1, maxDuration = 24, delayUnit = java.time.temporal.ChronoUnit.DAYS, durationUnit = HOURS)
public class RetryClientForValidationChronoUnit {

	@Retry(delay = 1, maxDuration = 61, delayUnit = ChronoUnit.HOURS, durationUnit = MINUTES)
    public Connection validA() {
        return null;
    }
	
    @Retry(delay = 1, maxDuration = 367, delayUnit = java.time.temporal.ChronoUnit.YEARS, durationUnit = ChronoUnit.DAYS)
    public Connection validB() {
        return null;
    }
    
    @Retry(delay = 30, jitter = 29, maxDuration = 1, delayUnit = ChronoUnit.MINUTES, jitterDelayUnit = ChronoUnit.MINUTES, durationUnit = ChronoUnit.HOURS)
    public Connection validC() {
        return null;
    }
	
    @Retry(delay = 1, maxDuration = 60, delayUnit = ChronoUnit.HOURS, durationUnit = MINUTES)
    public Connection invalidA() {
        return null;
    }

    @Retry(delay = 1, maxDuration = 365, delayUnit = java.time.temporal.ChronoUnit.YEARS, durationUnit = ChronoUnit.DAYS)
    public Connection invalidB() {
        return null;
    }

    @Retry(delay = 30, jitter = 30, maxDuration = 1, delayUnit = ChronoUnit.MINUTES, jitterDelayUnit = ChronoUnit.MINUTES, durationUnit = ChronoUnit.HOURS)
    public Connection invalidC() {
        return null;
    }

}