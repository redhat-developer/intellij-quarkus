/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.restclient.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.MicroProfileRestClientConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.MicroProfileRestClientErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

/**
 * Java diagnostics for MicroProfile RestClient.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/restclient/JavaDiagnosticsMicroProfileRestClientTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/restclient/JavaDiagnosticsMicroProfileRestClientTest.java</a>
 */
public class MicroProfileRestClientJavaDiagnosticsTest extends MavenModuleImportingTestCase {

    @Test
    public void testRestClientAnnotationMissingForFields() throws Exception {

        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/rest-client-quickstart"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams params = new MicroProfileJavaDiagnosticsParams();
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/restclient/Fields.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        params.setUris(Arrays.asList(uri));
        params.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d1 = d(12, 18, 26,
                "The corresponding `org.acme.restclient.MyService` interface does not have the @RegisterRestClient annotation. The field `service1` will not be injected as a CDI bean.",
                DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE, null);
        Diagnostic d2 = d(12, 28, 36,
                "The corresponding `org.acme.restclient.MyService` interface does not have the @RegisterRestClient annotation. The field `service2` will not be injected as a CDI bean.",
                DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE, null);
        Diagnostic d3 = d(15, 25, 52,
                "The Rest Client object should have the @RestClient annotation to be injected as a CDI bean.",
                DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
                MicroProfileRestClientErrorCode.RestClientAnnotationMissing);
        Diagnostic d4 = d(18, 25, 48,
                "The Rest Client object should have the @Inject annotation to be injected as a CDI bean.",
                DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
                MicroProfileRestClientErrorCode.InjectAnnotationMissing);
        Diagnostic d5 = d(20, 25, 61,
                "The Rest Client object should have the @Inject and @RestClient annotations to be injected as a CDI bean.",
                DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
                MicroProfileRestClientErrorCode.InjectAndRestClientAnnotationMissing);

        assertJavaDiagnostics(params, utils, //
                d1, //
                d2, //
                d3, //
                d4, //
                d5);

        /*String uri = javaFile.getLocation().toFile().toURI().toString();*/

        MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d3);
        assertJavaCodeAction(codeActionParams, utils, //
                ca(uri, "Insert @RestClient", MicroProfileCodeActionId.InsertRestClientAnnotation, d3, //
                        te(0, 0, 22, 10, "package org.acme.restclient;\n\nimport javax.inject.Inject;\n\nimport org.eclipse.microprofile.rest.client.inject.RestClient;\n\npublic class Fields {\n\n    public Country country;\n\n    @Inject\n    @RestClient\n    public MyService service1, service2;\n\n    @RestClient\n    @Inject\n    public CountriesService RestClientAnnotationMissing;\n\n    @RestClient\n    public CountriesService InjectAnnotationMissing;\n\n    public CountriesService RestClientAndInjectAnnotationMissing;\n}     \n          ")));

        codeActionParams = createCodeActionParams(uri, d4);
        assertJavaCodeAction(codeActionParams, utils, //
                ca(uri, "Insert @Inject", MicroProfileCodeActionId.InsertInjectAnnotation, d4, //
                        te(0, 0, 22, 10, "package org.acme.restclient;\n\nimport javax.inject.Inject;\n\nimport org.eclipse.microprofile.rest.client.inject.RestClient;\n\npublic class Fields {\n\n    public Country country;\n\n    @Inject\n    @RestClient\n    public MyService service1, service2;\n\n    @Inject\n    public CountriesService RestClientAnnotationMissing;\n\n    @Inject\n    @RestClient\n    public CountriesService InjectAnnotationMissing;\n\n    public CountriesService RestClientAndInjectAnnotationMissing;\n}     \n          ")));

        codeActionParams = createCodeActionParams(uri, d5);
        assertJavaCodeAction(codeActionParams, utils, //
                ca(uri, "Insert @Inject, @RestClient", MicroProfileCodeActionId.InsertInjectAndRestClientAnnotations, d5, //
                        te(0, 0, 22, 10, "package org.acme.restclient;\n\nimport javax.inject.Inject;\n\nimport org.eclipse.microprofile.rest.client.inject.RestClient;\n\npublic class Fields {\n\n    public Country country;\n\n    @Inject\n    @RestClient\n    public MyService service1, service2;\n\n    @Inject\n    public CountriesService RestClientAnnotationMissing;\n\n    @RestClient\n    public CountriesService InjectAnnotationMissing;\n\n    @RestClient\n    @Inject\n    public CountriesService RestClientAndInjectAnnotationMissing;\n}     \n          ")));
    }

    @Test
    public void testRestClientAnnotationMissingForFieldsJakarta() throws Exception {
        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/open-liberty"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams params = new MicroProfileJavaDiagnosticsParams();
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/com/demo/rest/injectAnnotation.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        params.setUris(Arrays.asList(uri));
        params.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d1 = d(10, 21, 40,
                "The corresponding `com.demo.rest.MyService` interface does not have the @RegisterRestClient annotation. The field `NoAnnotationMissing` will not be injected as a CDI bean.",
                DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE, null);
        Diagnostic d2 = d(13, 19, 42,
                "The Rest Client object should have the @Inject annotation to be injected as a CDI bean.",
                DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
                MicroProfileRestClientErrorCode.InjectAnnotationMissing);
        Diagnostic d3 = d(15, 19, 55,
                "The Rest Client object should have the @Inject and @RestClient annotations to be injected as a CDI bean.",
                DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
                MicroProfileRestClientErrorCode.InjectAndRestClientAnnotationMissing);

        assertJavaDiagnostics(params, utils, //
                d1, //
                d2, //
                d3);

        /* String uri = javaFile.getLocation().toFile().toURI().toString(); */

        MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d2);
        assertJavaCodeAction(codeActionParams, utils, //
                ca(uri, "Insert @Inject", MicroProfileCodeActionId.InsertInjectAnnotation, d2, //
                        te(0, 0, 17, 1, "package com.demo.rest;\n\nimport org.eclipse.microprofile.rest.client.inject.RestClient;\n\nimport jakarta.inject.Inject;\n\npublic class injectAnnotation {\n\n    @Inject\n    @RestClient\n    public MyService NoAnnotationMissing;\n\n    @Inject\n    @RestClient\n    public Service InjectAnnotationMissing;\n\n    public Service RestClientAndInjectAnnotationMissing;\n\n}")),
                ca(uri, "Generate OpenAPI Annotations for 'injectAnnotation'", MicroProfileCodeActionId.GenerateOpenApiAnnotations, d2, //
                        te(0, 0, 17, 1, "package com.demo.rest;\n\nimport org.eclipse.microprofile.rest.client.inject.RestClient;\n\nimport jakarta.inject.Inject;\n\npublic class injectAnnotation {\n\n    @Inject\n    @RestClient\n    public MyService NoAnnotationMissing;\n\n    @RestClient\n    public Service InjectAnnotationMissing;\n\n    public Service RestClientAndInjectAnnotationMissing;\n\n}")));

        codeActionParams = createCodeActionParams(uri, d3);
        assertJavaCodeAction(codeActionParams, utils, //
                ca(uri, "Insert @Inject, @RestClient", MicroProfileCodeActionId.InsertInjectAndRestClientAnnotations, d3, //
                        te(0, 0, 17, 1, "package com.demo.rest;\n\nimport org.eclipse.microprofile.rest.client.inject.RestClient;\n\nimport jakarta.inject.Inject;\n\npublic class injectAnnotation {\n\n    @Inject\n    @RestClient\n    public MyService NoAnnotationMissing;\n\n    @RestClient\n    public Service InjectAnnotationMissing;\n\n    @RestClient\n    @Inject\n    public Service RestClientAndInjectAnnotationMissing;\n\n}")),
                ca(uri, "Generate OpenAPI Annotations for 'injectAnnotation'", MicroProfileCodeActionId.GenerateOpenApiAnnotations, d3, //
                        te(0, 0, 17, 1, "package com.demo.rest;\n\nimport org.eclipse.microprofile.rest.client.inject.RestClient;\n\nimport jakarta.inject.Inject;\n\npublic class injectAnnotation {\n\n    @Inject\n    @RestClient\n    public MyService NoAnnotationMissing;\n\n    @RestClient\n    public Service InjectAnnotationMissing;\n\n    public Service RestClientAndInjectAnnotationMissing;\n\n}")));

    }

    @Test
    public void testRestClientAnnotationMissingForInterface() throws Exception {

        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/rest-client-quickstart"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams params = new MicroProfileJavaDiagnosticsParams();
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/restclient/MyService.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        params.setUris(Arrays.asList(uri));
        params.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d = d(2, 17, 26,
                "The interface `MyService` does not have the @RegisterRestClient annotation. The 1 fields references will not be injected as CDI beans.",
                DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
                MicroProfileRestClientErrorCode.RegisterRestClientAnnotationMissing);

        assertJavaDiagnostics(params, utils, //
                d);

        /*String uri = javaFile.getLocation().toFile().toURI().toString();*/
        MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
        assertJavaCodeAction(codeActionParams, utils, //
                ca(uri, "Insert @RegisterRestClient", MicroProfileCodeActionId.InsertRegisterRestClient, d, //
                        te(0, 0, 5, 0,
                                "package org.acme.restclient;\n\nimport org.eclipse.microprofile.rest.client.inject.RegisterRestClient;\n\n@RegisterRestClient\npublic interface MyService {\n\n}\n")));
    }

    @Test
    public void testRestClientAnnotationMissingForInterfaceJakarta() throws Exception {
        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/open-liberty"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams params = new MicroProfileJavaDiagnosticsParams();
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/com/demo/rest/MyService.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
        params.setUris(Arrays.asList(uri));
        params.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d = d(2, 17, 26,
                "The interface `MyService` does not have the @RegisterRestClient annotation. The 2 fields references will not be injected as CDI beans.",
                DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
                MicroProfileRestClientErrorCode.RegisterRestClientAnnotationMissing);

        assertJavaDiagnostics(params, utils, //
                d);

        MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
        assertJavaCodeAction(codeActionParams, utils, //
                ca(uri, "Insert @RegisterRestClient", MicroProfileCodeActionId.InsertRegisterRestClient, d, //
                        te(0, 0, 5, 0,
                                "package com.demo.rest;\n\nimport org.eclipse.microprofile.rest.client.inject.RegisterRestClient;\n\n@RegisterRestClient\npublic interface MyService {\n\n}\n")),
                ca(uri, "Generate OpenAPI Annotations for 'MyService'", MicroProfileCodeActionId.GenerateOpenApiAnnotations, d, //
                        te(0, 0, 5, 0,
                                "package com.demo.rest;\n\npublic interface MyService {\n\n}\n")));
    }
}
