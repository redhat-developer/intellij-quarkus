/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Qute configuration factory.
 */
public class QuteConfigurationFactory extends ConfigurationFactory {

    public QuteConfigurationFactory(@NotNull ConfigurationType type) {
        super(type);
    }

    public @NotNull String getId() {
        return this.getType().getId();
    }

    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new QuteRunConfiguration(project, this, "Qute");
    }

    public @Nullable Class<? extends BaseState> getOptionsClass() {
        return QuteRunConfigurationOptions.class;
    }

    public boolean isEditableInDumbMode() {
        return true;
    }
}
