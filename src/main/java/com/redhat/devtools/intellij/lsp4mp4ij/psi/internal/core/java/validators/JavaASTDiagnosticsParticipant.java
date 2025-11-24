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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.java.validators;

import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaDiagnosticsParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidator;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The java diagnostic participant which visit one time a given AST compilation
 * unit and loops for each {@link JavaASTValidator} registered with
 * "org.eclipse.lsp4mp.jdt.core.javaASTValidators" extension point to report LSP
 * {@link Diagnostic}.
 * 
 * @author Angelo ZERR
 *
 */
public class JavaASTDiagnosticsParticipant implements IJavaDiagnosticsParticipant {

	@Override
	public void collectDiagnostics(JavaDiagnosticsContext context) {
		// Collect the list of JavaASTValidator which are adapted for the current AST
		// compilation unit to validate.
		Collection<JavaASTValidator> validators = JavaASTValidatorRegistry.getInstance().getValidators(context);
		if (!validators.isEmpty()) {
			// Visit the AST compilation unit and process each validator.
			PsiFile ast = context.getASTRoot();
			ast.accept(new MultiASTVisitor(validators));
		}
	}

}
