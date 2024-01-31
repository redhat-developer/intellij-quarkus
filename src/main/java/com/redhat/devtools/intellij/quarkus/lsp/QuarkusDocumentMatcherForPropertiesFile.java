/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lsp;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Quarkus document matcher for application.properties, microprofile-config.properties
 * file which checks that the properties file belongs to a Quarkus project.
 */
public class QuarkusDocumentMatcherForPropertiesFile extends AbstractQuarkusDocumentMatcher {

    @Override
    public boolean match(@NotNull VirtualFile file, @NotNull Project fileProject) {
        if (!matchFile(file, fileProject)) {
            return false;
        }
        return super.match(file, fileProject);
    }

    private boolean matchFile(VirtualFile file, Project fileProject) {
        return QuarkusModuleUtil.isQuarkusPropertiesFile(file, fileProject) || QuarkusModuleUtil.isQuarkusYAMLFile(file, fileProject);
    }
}