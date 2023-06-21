/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.psi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.qute.psi.internal.java.QuarkusIntegrationForQute;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.PublishDiagnosticsParams;

import com.redhat.qute.commons.QuteJavaCodeLensParams;
import com.redhat.qute.commons.QuteJavaDiagnosticsParams;
import com.redhat.qute.commons.QuteJavaDocumentLinkParams;

/**
 * Qute support for Java file.
 *
 * @author Angelo ZERR
 */
public class QuteSupportForJava {

    private static final QuteSupportForJava INSTANCE = new QuteSupportForJava();

    public static QuteSupportForJava getInstance() {
        return INSTANCE;
    }

    public List<? extends CodeLens> codeLens(QuteJavaCodeLensParams params, IPsiUtils utils, ProgressIndicator monitor) {
        String uri = params.getUri();

        Module javaProject = QuteSupportForTemplate.getJavaProjectFromTemplateFile(uri, utils);
        if (javaProject == null) {
            return Collections.emptyList();
        }

        final var refinedUtils = utils.refine(javaProject);
        PsiFile typeRoot = resolveTypeRoot(uri, refinedUtils, monitor);
        if (monitor.isCanceled()) {
            return Collections.emptyList();
        }
        return QuarkusIntegrationForQute.codeLens(typeRoot, refinedUtils, monitor);
    }

    public List<PublishDiagnosticsParams> diagnostics(QuteJavaDiagnosticsParams params, IPsiUtils utils,
                                                      ProgressIndicator monitor) {
        List<String> uris = params.getUris();
        if (uris == null || uris.isEmpty()) {
            return Collections.emptyList();
        }

        Module javaProject = QuteSupportForTemplate.getJavaProjectFromTemplateFile(uris.get(0), utils);
        if (javaProject == null) {
            return Collections.emptyList();
        }

        utils = utils.refine(javaProject);

        List<PublishDiagnosticsParams> publishDiagnostics = new ArrayList<PublishDiagnosticsParams>();
        for (String uri : uris) {
            if (monitor.isCanceled()) {
                return Collections.emptyList();
            }
            List<Diagnostic> diagnostics = new ArrayList<>();
            PublishDiagnosticsParams publishDiagnostic = new PublishDiagnosticsParams(uri, diagnostics);
            publishDiagnostics.add(publishDiagnostic);
            PsiFile typeRoot = resolveTypeRoot(uri, utils, monitor);
            QuarkusIntegrationForQute.diagnostics(typeRoot, diagnostics, utils, monitor);
        }
        if (monitor.isCanceled()) {
            return Collections.emptyList();
        }
        return publishDiagnostics;
    }

    public List<DocumentLink> documentLink(QuteJavaDocumentLinkParams params, IPsiUtils utils,
                                           ProgressIndicator monitor) {
        String uri = params.getUri();

        Module javaProject = QuteSupportForTemplate.getJavaProjectFromTemplateFile(uri, utils);
        if (javaProject == null) {
            return Collections.emptyList();
        }

        utils = utils.refine(javaProject);

        final var refinedUtils = utils.refine(javaProject);
        PsiFile typeRoot = resolveTypeRoot(uri, refinedUtils, monitor);
        if (monitor.isCanceled()) {
            return Collections.emptyList();
        }
        return QuarkusIntegrationForQute.documentLink(typeRoot, refinedUtils, monitor);
    }

    /**
     * Given the uri returns a {@link PsiFile}. May return null if it can not
     * associate the uri with a Java file ot class file.
     *
     * @param uri
     * @param utils   JDT LS utilities
     * @param monitor the progress monitor
     * @return compilation unit
     */
    private static PsiFile resolveTypeRoot(String uri, IPsiUtils utils, ProgressIndicator monitor) {
        //utils.waitForLifecycleJobs(monitor);
        final PsiFile unit = utils.resolveCompilationUnit(uri);
        PsiFile classFile = null;
        if (unit == null) {
            classFile = utils.resolveClassFile(uri);
            if (classFile == null) {
                return null;
            }
        } else {
            if (!unit.isValid() || monitor.isCanceled()) {
                return null;
            }
        }
        return unit != null ? unit : classFile;
    }

}
