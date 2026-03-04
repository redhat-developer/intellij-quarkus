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
package com.redhat.devtools.intellij.qute.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutor;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.*;

/**
 * Qute language substitutor to force some language file (ex: HTML, YAML, etc.) to the "_Qute" language when:
 * <ul>
 *     <li>the file is hosted in a Qute project.</li>
 *     <li>the file is located in the {@code src/main/resources/templates} folder.</li>
 * </ul>
 *
 * <p>Cache invalidation is handled in {@link PsiQuteProjectUtils}.</p>
 */
public class QuteLanguageSubstitutor extends LanguageSubstitutor {

    /**
     * Returns the Qute language if the given file is a Qute template, null otherwise.
     *
     * <p>Returns null when called on the EDT to avoid slow operations
     * (e.g. triggered by breadcrumbs initialization). Primary language resolution
     * (syntax highlighting, PSI, LSP) runs off EDT and will still correctly identify
     * Qute templates.</p>
     *
     * @param file    the virtual file to check, must not be null.
     * @param project the current project, must not be null.
     * @return {@link QuteLanguage#INSTANCE} if the file is a Qute template, null otherwise.
     */
    @Override
    public @Nullable Language getLanguage(@NotNull VirtualFile file, @NotNull Project project) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            return null;
        }

        if (maybeBinaryQuteTemplate(file)) {
            // File is inside a JAR archive in 'templates' JAR entry
            return hasQuteSupport(project) ? QuteLanguage.INSTANCE : null;
        }

        Module module = LSPIJUtils.getModule(file, project);
        if (module != null) {
            if (hasQuteSupport(module) && isQuteTemplate(file, module)) {
                return QuteLanguage.INSTANCE;
            }
        }
        return null;
    }
}