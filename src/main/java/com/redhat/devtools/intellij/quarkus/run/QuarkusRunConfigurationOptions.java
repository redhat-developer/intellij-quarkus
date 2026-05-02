/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.run;

import com.intellij.execution.configurations.ModuleBasedConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;

import java.util.HashMap;
import java.util.Map;

public class QuarkusRunConfigurationOptions extends ModuleBasedConfigurationOptions {
    private final StoredProperty<String> profileProperty = string("").provideDelegate(this, "profile");

    private final StoredProperty<Map<String, String>> envProperty = this.<String, String>map().provideDelegate(this, "env");

    private final StoredProperty<String> programParametersProperty = string("").provideDelegate(this, "programParameters");

    private final StoredProperty<Boolean> passParentEnvsProperty = property(true).provideDelegate(this, "passParentEnvs");

    private final StoredProperty<String> workingDirectoryProperty = string("").provideDelegate(this, "workingDirectory");

    public String getProfile() {
        return profileProperty.getValue(this);
    }

    public void setProfile(String profile) {
        profileProperty.setValue(this, profile);
    }

    public Map<String, String> getEnv() {
        Map<String, String> env = envProperty.getValue(this);
        if (env == null) {
            env = new HashMap<>();
        }
        return env;
    }

    public void setEnv(Map<String, String> env) {
        envProperty.setValue(this, env);
    }

    public String getProgramParameters() {
        return programParametersProperty.getValue(this);
    }

    public void setProgramParameters(String programParameters) {
        programParametersProperty.setValue(this, programParameters);
    }

    public boolean isPassParentEnvs() {
        return passParentEnvsProperty.getValue(this);
    }

    public void setPassParentEnvs(boolean passParentEnvs) {
        passParentEnvsProperty.setValue(this, passParentEnvs);
    }

    public String getWorkingDirectory() {
        return workingDirectoryProperty.getValue(this);
    }

    public void setWorkingDirectory(String workingDirectory) {
        workingDirectoryProperty.setValue(this, workingDirectory);
    }
}
