/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;

import org.eclipse.lsp4mp.commons.DocumentFormat;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.AbstractJavaContext;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsSettings;

import java.util.Collections;

/**
 * Java diagnostics context for a given compilation unit.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/diagnostics/JavaDiagnosticsContext.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/diagnostics/JavaDiagnosticsContext.java</a>
 *
 */
public class JavaDiagnosticsContext extends AbstractJavaContext {

	private final DocumentFormat documentFormat;

	private final MicroProfileJavaDiagnosticsSettings settings;

	public JavaDiagnosticsContext(String uri, PsiFile typeRoot, IPsiUtils utils, Module module, DocumentFormat documentFormat, MicroProfileJavaDiagnosticsSettings settings) {
		super(uri, typeRoot, utils, module);
		this.documentFormat = documentFormat;
		if (settings == null) {
			this.settings = new MicroProfileJavaDiagnosticsSettings(Collections.emptyList());
		} else {
			this.settings = settings;
		}
	}

	public DocumentFormat getDocumentFormat() {
		return documentFormat;
	}

	/**
	 * Returns the MicroProfileJavaDiagnosticsSettings.
	 *
	 * Should not be null.
	 *
	 * @return the MicroProfileJavaDiagnosticsSettings
	 */
	public MicroProfileJavaDiagnosticsSettings getSettings() {
		return this.settings;
	}

	public Diagnostic createDiagnostic(String uri, String message, Range range, String source, IJavaErrorCode code) {
		return createDiagnostic(uri, message, range, source, code, DiagnosticSeverity.Warning);
	}

	public Diagnostic createDiagnostic(String uri, String message, Range range, String source, IJavaErrorCode code,
									   DiagnosticSeverity severity) {
		Diagnostic diagnostic = new Diagnostic();
		diagnostic.setSource(source);
		diagnostic.setMessage(message);
		diagnostic.setSeverity(severity);
		diagnostic.setRange(range);
		if (code != null) {
			diagnostic.setCode(code.getCode());
		}
		return diagnostic;
	}
}
