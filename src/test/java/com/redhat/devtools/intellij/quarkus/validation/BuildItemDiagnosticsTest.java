/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.quarkus.validation;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import com.redhat.devtools.intellij.quarkus.psi.internal.builditems.QuarkusBuildItemErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

public class BuildItemDiagnosticsTest extends MavenModuleImportingTestCase {

    public void testBuildItemClassifier() throws Exception {
        Module module = createMavenModule(new File("projects/quarkus/projects/maven/quarkus-builditems"));

        { // Bad BuildItem shows error
            MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
            String uri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/builditems/BadBuildItem.java").toASCIIString();
            diagnosticsParams.setUris(Arrays.asList(uri));
            diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

            Diagnostic d = d(4, 0, 49,
                    "BuildItem class `org.acme.builditems.BadBuildItem` must either be declared final or abstract",
                    DiagnosticSeverity.Error, QuarkusConstants.QUARKUS_DIAGNOSTIC_SOURCE,
                    QuarkusBuildItemErrorCode.InvalidModifierBuildItem);
            assertJavaDiagnostics(diagnosticsParams, PsiUtilsLSImpl.getInstance(myProject), d);
        }


        { // Good BuildItem shows no error
            MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
            String uri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/builditems/GoodBuildItem.java").toASCIIString();
            diagnosticsParams.setUris(Arrays.asList(uri));
            diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);
            assertJavaDiagnostics(diagnosticsParams, PsiUtilsLSImpl.getInstance(myProject));
        }

    }
}
