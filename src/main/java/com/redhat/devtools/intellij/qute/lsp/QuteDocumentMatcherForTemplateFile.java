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
package com.redhat.devtools.intellij.qute.lsp;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.*;

/**
 * Qute document matcher for html, yaml, txt, json files which checks that the file belongs to a Qute project and it is hosted in the template folder (ex : src/main/resources/templates)
 */
public class QuteDocumentMatcherForTemplateFile extends AbstractQuteDocumentMatcher {

    @Override
    public boolean match(@NotNull VirtualFile file,
                         @NotNull Project fileProject) {
        if (maybeBinaryQuteTemplate(file)) {
            // File is inside a JAR archive in 'templates' JAR entry
            return hasQuteSupport(fileProject);
        }
        // File is a source file (ex: src/main/resources/templates/foo.html)
        return super.match(file, fileProject);
    }

    @Override
    protected boolean match(@NotNull VirtualFile file,
                            @NotNull Module module) {
        return isQuteTemplate(file, module);
    }
}