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

import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.hasQuteSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentLink;

/**
 * Quarkus integration for Qute.
 * 
 * @author Angelo ZERR
 *
 */
public class QuarkusIntegrationForQute {

	public static List<? extends CodeLens> codeLens(PsiFile typeRoot, IPsiUtils utils, ProgressIndicator monitor) {
		if (typeRoot == null || !hasQuteSupport(utils.getModule())) {
			return Collections.emptyList();
		}
		List<CodeLens> lenses = new ArrayList<>();
		PsiFile cu = typeRoot;
		cu.accept(new QuteJavaCodeLensCollector(typeRoot, lenses, utils, monitor));
		return lenses;
	}

	public static void diagnostics(PsiFile typeRoot, List<Diagnostic> diagnostics, IPsiUtils utils,
			ProgressIndicator monitor) {
		if (typeRoot == null || !hasQuteSupport(utils.getModule())) {
			return;
		}
		PsiFile cu = typeRoot;
		cu.accept(new QuteJavaDiagnosticsCollector(typeRoot, diagnostics, utils, monitor));
	}

	public static List<DocumentLink> documentLink(PsiFile typeRoot, IPsiUtils utils,
			ProgressIndicator monitor) {
		if (typeRoot == null || !hasQuteSupport(utils.getModule())) {
			return Collections.emptyList();
		}
		List<DocumentLink> links = new ArrayList<>();
		PsiFile cu = typeRoot;
		cu.accept(new QuteJavaDocumentLinkCollector(typeRoot, links, utils, monitor));
		return links;
	}
}
