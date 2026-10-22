/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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
package com.redhat.microprofile.psi.quarkus.flow;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.psi.internal.providers.QuarkusConfigSourceProvider;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenModuleImportingTestCase;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenProjectName;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;

/**
 * Tests the native {@code PsiReference} that lets Ctrl/Cmd-click (Go to Declaration) navigate
 * from {@code @Identifier("...")} on a {@code Flow}-typed field to the matching workflow file,
 * matched by its {@code document.namespace}/{@code document.name}/{@code document.version}
 * content - not by file name.
 */
public class QuarkusFlowReferenceTest extends QuarkusMavenModuleImportingTestCase {

	@Test
	public void testNavigatesToWorkflowFileByDocumentMetadata() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.flow_quickstart);
		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE,
				"quarkus.flow.definitions.naming-strategy=spec\n", javaProject);
		PsiFile javaFile = findJavaFile(javaProject, "src/main/java/org/acme/flow/FlowBean.java");

		// Inside "flow:for-task-name-collision"
		PsiReference reference = findReferenceContaining(javaFile, "for-task-name-collision");
		assertNotNull(reference);

		PsiElement resolved = reference.resolve();
		assertNotNull(resolved);
		assertTrue(resolved instanceof PsiFile);
		assertEquals("sample.yaml", ((PsiFile) resolved).getName());
	}

	@Test
	public void testVersionlessIdentifierResolvesToHighestVersion() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.flow_quickstart);
		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE,
				"quarkus.flow.definitions.naming-strategy=spec\n", javaProject);
		PsiFile javaFile = findJavaFile(javaProject, "src/main/java/org/acme/flow/FlowBean.java");

		// Inside "flow:multi-version"
		PsiReference reference = findReferenceContaining(javaFile, "multi-version");
		assertNotNull(reference);

		PsiElement resolved = reference.resolve();
		assertNotNull(resolved);
		assertEquals("multi-v2.yaml", ((PsiFile) resolved).getName());
	}

	@Test
	public void testClassNamingStrategyIncludesVersionSegment() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.flow_quickstart);
		// The "class" naming strategy's generated bean identifier is
		// `namespace.vX_Y_Z.PascalCaseName` - it includes a version package segment, unlike the
		// "spec" strategy there is no versionless convenience form.
		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE,
				"quarkus.flow.definitions.naming-strategy=class\n", javaProject);
		PsiFile javaFile = findJavaFile(javaProject, "src/main/java/org/acme/flow/FlowClassStrategyBean.java");

		// Inside "flow.v1_0_0.MultiVersion"
		PsiReference reference = findReferenceContaining(javaFile, "flow.v1_0_0.MultiVersion");
		assertNotNull(reference);

		PsiElement resolved = reference.resolve();
		assertNotNull(resolved);
		assertEquals("multi-v1.yaml", ((PsiFile) resolved).getName());
	}

	@Test
	public void testNonFlowIdentifierHasNoReference() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.flow_quickstart);
		PsiFile javaFile = findJavaFile(javaProject, "src/main/java/org/acme/flow/FlowBean.java");

		// Inside "unrelated-bean" (a String field, not Flow)
		PsiReference reference = findReferenceContaining(javaFile, "unrelated-bean");
		assertNull(reference);
	}

	private PsiFile findJavaFile(Module javaProject, String relativePath) throws Exception {
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
		String uri = com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert
				.getFileUri(relativePath, javaProject);
		var virtualFile = utils.findFile(uri);
		assertNotNull(virtualFile);
		PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(virtualFile);
		assertNotNull(psiFile);
		return psiFile;
	}

	/**
	 * Mirrors what a real Ctrl/Cmd-click does: {@code PsiFile.findReferenceAt} climbs from the
	 * leaf token at the given offset up to whichever ancestor element (here, the enclosing
	 * {@code PsiLiteralExpression}) actually has a registered reference - calling
	 * {@code element.getReferences()} directly on the leaf token would not find it.
	 */
	private PsiReference findReferenceContaining(PsiFile javaFile, String substring) {
		String text = javaFile.getText();
		int offset = text.indexOf(substring);
		assertTrue("substring not found: " + substring, offset >= 0);
		return javaFile.findReferenceAt(offset);
	}

}
