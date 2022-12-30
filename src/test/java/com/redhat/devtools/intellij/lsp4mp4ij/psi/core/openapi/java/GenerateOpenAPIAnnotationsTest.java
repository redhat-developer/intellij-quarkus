/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.openapi.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaCodeAction;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.ca;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.createCodeActionParams;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.te;

/**
 * Code action for generating MicroProfile OpenAPI annotations.
 *
 * @author Benson Ning
 *
 */
public class GenerateOpenAPIAnnotationsTest extends MavenModuleImportingTestCase {

	@Test
	public void testGenerateOpenAPIAnnotationsAction() throws Exception {
		Module javaProject = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-openapi"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		File javaFile = new File(ModuleUtilCore.getModuleDirPath(javaProject),
				"src/main/java/org/acme/openapi/NoOperationAnnotation.java");
		String uri = javaFile.toURI().toString();
		Diagnostic d = new Diagnostic();
		Position start = new Position(8, 23);
		d.setRange(new Range(start, start));
		MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);

		String newText = "package org.acme.openapi;\n\nimport org.eclipse.microprofile.openapi.annotations.Operation;\n\nimport java.util.Properties;\nimport javax.enterprise.context.RequestScoped;\nimport javax.ws.rs.GET;\nimport javax.ws.rs.Path;\nimport javax.ws.rs.core.Response;\n\n@RequestScoped\n@Path(\"/systems\")\npublic class NoOperationAnnotation {\n\n    @Operation(summary = \"\", description = \"\")\n    @GET\n    public Response getMyInformation(String hostname) {\n        return Response.ok(listContents()).build();\n    }\n\n    @Operation(summary = \"\", description = \"\")\n    @GET\n    public Response getPropertiesForMyHost() {\n        return Response.ok().build();\n    }\n\n    @Operation(summary = \"\", description = \"\")\n    private Properties listContents() {\n        Properties info = new Properties();\n        info.setProperty(\"Name\", \"APITest\");\n        info.setProperty(\"Desc\", \"API Test\");\n        return info;\n    }\n\n}";
		assertJavaCodeAction(codeActionParams, utils,
				ca(uri, "Generate OpenAPI Annotations for 'NoOperationAnnotation'", d,
						te(0, 0, 34, 1, newText))
		);
	}

}
