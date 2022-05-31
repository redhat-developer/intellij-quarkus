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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaErrorCode;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations.AnnotationAttributeRule;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;

import java.util.List;
import java.util.logging.Logger;

/**
 * 
 * JDT visitor to process validation and report LSP diagnostics by visiting AST.
 * 
 * To manage validation by visiting AST, you need:
 * 
 * <ul>
 * <li>create class which extends {@link JavaASTValidator}</li>
 * <li>register this class with the
 * "org.eclipse.lsp4mp.jdt.core.javaASTValidators" extension point:
 * 
 * <code>
 *    <extension point="org.eclipse.lsp4mp.jdt.core.javaASTValidators">
      <!-- Java validation for the MicroProfile @Fallback / @Asynchronous annotations -->
      <validator class=
"org.eclipse.lsp4mp.jdt.internal.faulttolerance.java.MicroProfileFaultToleranceASTValidator" />
   </extension>

 * </code></li>
 * </ul>
 * 
 * 
 * @author Angelo ZERR
 *
 */
public class JavaASTValidator extends JavaRecursiveElementVisitor implements Cloneable {

	private static final Logger LOGGER = Logger.getLogger(JavaASTValidator.class.getName());

	public static final ExtensionPointName<JavaASTValidatorExtensionPointBean> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.javaASTValidator.validator");

	private List<Diagnostic> diagnostics;

	private JavaDiagnosticsContext context;

	protected static String validate(String valueAsString, AnnotationAttributeRule attributeRule) {
		if (attributeRule != null) {
			return attributeRule.validate(valueAsString);
		}
		return null;
	}

	/**
	 * Initialize the visitor with a given context and diagnostics to update.
	 * 
	 * @param context     the context.
	 * @param diagnostics the diagnostics to update.
	 */
	public void initialize(JavaDiagnosticsContext context, List<Diagnostic> diagnostics) {
		this.context = context;
		this.diagnostics = diagnostics;
	}

	/**
	 * Returns true if diagnostics must be collected for the given context and false
	 * otherwise.
	 *
	 * <p>
	 * Collection is done by default. Participants can override this to check if
	 * some classes are on the classpath before deciding to process the collection.
	 * </p>
	 *
	 * @param context the     java diagnostics context
	 * @return true if diagnostics must be collected for the given context and false
	 *         otherwise.
	 *
	 */
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
		return true;
	}

	public Diagnostic addDiagnostic(String message, String source, PsiElement node, IJavaErrorCode code,
									DiagnosticSeverity severity) {
		return addDiagnostic(message, source, node.getTextOffset(), node.getTextLength(), code, severity);
	}

	public Diagnostic addDiagnostic(String message, String source, int offset, int length, IJavaErrorCode code,
			DiagnosticSeverity severity) {
			String fileUri = context.getUri();
			PsiFile openable = context.getTypeRoot();
			Range range = context.getUtils().toRange(openable, offset, length);
			Diagnostic d = context.createDiagnostic(fileUri, message, range, source, code, severity);
			diagnostics.add(d);
			return d;
	}

	public JavaDiagnosticsContext getContext() {
		return context;
	}
}
