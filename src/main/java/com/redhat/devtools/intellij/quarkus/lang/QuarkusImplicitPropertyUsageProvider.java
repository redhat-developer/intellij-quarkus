/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lang;

import com.intellij.lang.properties.codeInspection.unused.ImplicitPropertyUsageProvider;
import com.intellij.lang.properties.psi.Property;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;

public class QuarkusImplicitPropertyUsageProvider implements ImplicitPropertyUsageProvider {
    @Override
    public boolean isUsed(Property property) {
        return QuarkusModuleUtil.isQuarkusPropertiesFile(property.getContainingFile().getVirtualFile(), property.getProject());
    }
}
