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
package com.redhat.devtools.intellij.qute.psi.internal.java;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralValue;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.qute.psi.utils.TemplatePathInfo;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;

import java.util.List;

/**
 * Report diagnostics error for non existing Qute template for:
 *
 * <ul>
 * <li>declared method which have class annotated with @CheckedTemplate.</li>
 * <li>declared field which have Template as type.</li>
 * </ul>
 *
 * @author Angelo ZERR
 */
public class QuteJavaDiagnosticsCollector extends AbstractQuteTemplateLinkCollector {

    public static final String QUTE_SOURCE = "qute";

    private final List<Diagnostic> diagnostics;

    public QuteJavaDiagnosticsCollector(PsiFile typeRoot, List<Diagnostic> diagnostics, IPsiUtils utils,
                                        ProgressIndicator monitor) {
        super(typeRoot, utils, monitor);
        this.diagnostics = diagnostics;
    }

    @Override
    protected void collectTemplateLink(PsiElement fieldOrMethod, PsiLiteralValue locationAnnotation, PsiClass type, String className,
                                       String fieldOrMethodName, String location, VirtualFile templateFile, TemplatePathInfo templatePathInfo) {
        QuteErrorCode error = getQuteErrorCode(templatePathInfo, templateFile);
        if (error == null) {
            return;
        }
        String path = createPath(className, fieldOrMethodName, location);
        String fragmentId = templatePathInfo.getFragmentId();
        if (templatePathInfo.hasFragment() && path.endsWith(fragmentId)) {
            // Adjust path by removing fragment information
            path = path.substring(0, path.length() - (1 /* '$') */ + fragmentId.length()));
        }
        Range range = createRange(locationAnnotation != null ? locationAnnotation : fieldOrMethod);

        switch (error) {
            case FragmentNotDefined: {
                Diagnostic diagnostic = createDiagnostic(range, DiagnosticSeverity.Error, error, "", path);
                this.diagnostics.add(diagnostic);
                break;
            }
            case NoMatchingTemplate: {
                String fullQualifiedName = PsiTypeUtils.getSourceType(type);
                Diagnostic diagnostic = createDiagnostic(range, DiagnosticSeverity.Error, error, path, fullQualifiedName);
                this.diagnostics.add(diagnostic);
                break;
            }
        }
    }

    private static QuteErrorCode getQuteErrorCode(TemplatePathInfo templatePathInfo, VirtualFile templateFile) {
        if (!templatePathInfo.isValid()) {
            // It is an empty fragment which is not valid, report an error
            // Fragment [] not defined in template GreetingResource/myTemplate.html
            return QuteErrorCode.FragmentNotDefined;
        }
        if (templateFile ==null || !templateFile.exists()) {
            // No template matching the path HelloResource/index could be found for:
            // org.acme.HelloResource$Templates.index
            return QuteErrorCode.NoMatchingTemplate;
        }
        return null;
    }

    private static String createPath(String className, String fieldOrMethodName, String location) {
        if (location != null) {
            return location;
        }
        if (className == null) {
            return fieldOrMethodName;
        }
        return className + '/' + fieldOrMethodName;
    }

    private static Diagnostic createDiagnostic(Range range, DiagnosticSeverity severity, IQuteErrorCode errorCode,
                                               Object... arguments) {
        String message = errorCode.getMessage(arguments);
        return new Diagnostic(range, message, severity, QUTE_SOURCE, errorCode.getCode());
    }
}
