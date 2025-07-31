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

import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.openapi.util.NotNullLazyValue;
import com.redhat.devtools.intellij.quarkus.lang.QuarkusIcons;

/**
 * Qute configuration type.
 */
public class QuteConfigurationType extends ConfigurationTypeBase {

    public static final String ID = "QuteConfiguration";

    public static QuteConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(QuteConfigurationType.class);
    }

    QuteConfigurationType() {
        super(ID,
                "Qute",
                "Qute debugger",
                NotNullLazyValue.createValue(() -> QuarkusIcons.Quarkus));
        addFactory(new QuteConfigurationFactory(this));
    }
}
