/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.run.dashboard;

import com.intellij.execution.dashboard.RunDashboardDefaultTypesProvider;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfigurationType;
import com.redhat.devtools.intellij.quarkus.settings.UserDefinedQuarkusSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Add automatically the {@link QuarkusRunConfigurationType#ID} type in the run dashboard when
 * the option 'Create "Quarkus Dev Mode" run configuration on project import' is activated.
 */
public class QuarkusRunDashboardDefaultTypesProvider implements RunDashboardDefaultTypesProvider {

    @Override
    public @NotNull Collection<String> getDefaultTypeIds(@NotNull Project project) {
        if (!UserDefinedQuarkusSettings.getInstance(project).isCreateQuarkusRunConfigurationOnProjectImport()) {
            return Collections.emptyList();
        }
        return Collections.singleton(QuarkusRunConfigurationType.ID);
    }
}
