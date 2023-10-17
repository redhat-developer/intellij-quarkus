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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;

import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.isQuteTemplate;

/**
 * Qute document matcher for html, yaml, txt, json files which checks that the file belongs to a Qute project and it is hosted in the template folder (ex : src/main/resources/templates)
 */
public class QuteDocumentMatcherForTemplateFile extends AbstractQuteDocumentMatcher {

    @Override
    public boolean match(VirtualFile file, Project fileProject) {
        if (!super.match(file, fileProject)) {
            return false;
        }
        return isQuteTemplate(file, LSPIJUtils.getModule(file, fileProject));
    }
}