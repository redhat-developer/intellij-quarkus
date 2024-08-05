/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
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
package com.redhat.microprofile.psi.quarkus.jaxrs;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertWorkspaceSymbols;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.si;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.r;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.LSP4MPMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileMavenProjectName;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.junit.Test;

/**
 * Tests for <code>JaxRsWorkspaceSymbolParticipantTest</code>.
 */
public class JaxRsWorkspaceSymbolParticipantTest extends LSP4MPMavenModuleImportingTestCase {

	@Test
	public void testConfigQuickstart() throws Exception {
		Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_quickstart);

		assertWorkspaceSymbols(javaProject, PsiUtilsLSImpl.getInstance(myProject), //
				si("@/greeting/hello4: GET", r(40, 18, 24)), //
				si("@/greeting/constructor: GET", r(34, 18, 23)), //
				si("@/greeting/hello: GET", r(33, 18, 24)), //
				si("@/greeting: GET", r(26, 18, 23)), //
				si("@/greeting/method: GET", r(38, 18, 23)), //
				si("@/greeting/hello5: PATCH", r(46, 18, 24)));
	}

	@Test
	public void testOpenLiberty() throws Exception {
		Module javaProject = loadMavenProject(MicroProfileMavenProjectName.open_liberty);

		assertWorkspaceSymbols(javaProject, PsiUtilsLSImpl.getInstance(myProject), //
				si("@/api/api/resource: GET", r(13, 15, 20)));
	}

}