/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.quarkus.buildtool.maven;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.redhat.devtools.intellij.quarkus.buildtool.BuildToolDelegate;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;

/**
 * Program runner to run a Maven configuration.
 */
public class QuarkusMavenRunProgramRunner extends DefaultJavaProgramRunner {

    private static final String RUNNER_ID = "QuarkusMavenRunProgramRunner";

    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(String executorId, RunProfile profile) {
        if (!executorId.equals(DefaultRunExecutor.EXECUTOR_ID)) {
            return false;
        }
        // Running...
        if (profile instanceof QuarkusRunConfiguration quarkusRunConfiguration) {
            // returns true if the profile is a QuarkusRunConfiguration which wraps a Maven configuration
            BuildToolDelegate delegate = BuildToolDelegate.getDelegate(quarkusRunConfiguration.getModule());
            return (delegate instanceof MavenToolDelegate);
        }
        return false;
    }

}
