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
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class QuarkusRunConfigurationType implements ConfigurationType {
    private static final String ID = "QuarkusRunConfiguration";

    @NotNull
    @Override
    public String getDisplayName() {
        return "Quarkus";
    }

    @Nls
    @Override
    public String getConfigurationTypeDescription() {
        return "Quarkus run configuration";
    }

    @Override
    public Icon getIcon() {
        return IconLoader.findIcon("/quarkus_icon_rgb_16px_default.png", QuarkusRunConfigurationType.class);
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[] {new QuarkusRunConfigurationFactory(this)};
    }
}
