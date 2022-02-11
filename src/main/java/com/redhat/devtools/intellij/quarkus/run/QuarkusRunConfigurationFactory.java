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

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuarkusRunConfigurationFactory extends ConfigurationFactory {
    public QuarkusRunConfigurationFactory(ConfigurationType type) {
        super(type);
    }

    @NotNull
    @Override
    public String getId() {
        return getType().getId();
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new QuarkusRunConfiguration(project, this, "Quarkus");
    }

    @Nullable
    @Override
    public Class<? extends BaseState> getOptionsClass() {
        return QuarkusRunConfigurationOptions.class;
    }
}
