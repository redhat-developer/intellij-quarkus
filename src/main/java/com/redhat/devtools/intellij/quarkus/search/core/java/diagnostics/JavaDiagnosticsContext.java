/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.core.java.diagnostics;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;

import org.eclipse.lsp4mp.commons.DocumentFormat;
import com.redhat.devtools.intellij.quarkus.search.core.java.AbstractJavaContext;

/**
 * Java diagnostics context for a given compilation unit.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/diagnostics/JavaDiagnosticsContext.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/diagnostics/JavaDiagnosticsContext.java</a>
 *
 */
public class JavaDiagnosticsContext extends AbstractJavaContext {

	private final DocumentFormat documentFormat;

	public JavaDiagnosticsContext(String uri, PsiFile typeRoot, IPsiUtils utils, Module module, DocumentFormat documentFormat) {
		super(uri, typeRoot, utils, module);
		this.documentFormat = documentFormat;
	}

	public DocumentFormat getDocumentFormat() {
		return documentFormat;
	}

	public Diagnostic createDiagnostic(String uri, String message, Range range, String source, IJavaErrorCode code) {
		Diagnostic diagnostic = new Diagnostic();
		diagnostic.setSource(source);
		diagnostic.setMessage(message);
		diagnostic.setSeverity(DiagnosticSeverity.Warning);
		diagnostic.setRange(range);
		if (code != null) {
			diagnostic.setCode(code.getCode());
		}
		return diagnostic;
	}

}
