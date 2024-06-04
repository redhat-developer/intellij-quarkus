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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidator;

import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Multiple JDT AST visitor.
 * 
 * @author Angelo ZERR
 *
 */
public class MultiASTVisitor extends JavaRecursiveElementVisitor {

	private static final Logger LOGGER = Logger.getLogger(MultiASTVisitor.class.getName());
	private final Collection<JavaASTValidator> visitors;

	public MultiASTVisitor(Collection<JavaASTValidator> visitors) {
		this.visitors = visitors;
	}

	@Override
	public void visitAnnotation(PsiAnnotation node) {
		for (JavaRecursiveElementVisitor visitor : visitors) {
			try {
				visitor.visitAnnotation(node);
			} catch (ProcessCanceledException e) {
				//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
				//TODO delete block when minimum required version is 2024.2
				throw e;
			} catch (IndexNotReadyException | CancellationException e) {
				throw e;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error while visiting node with " + visitor.getClass().getName(), e);
			}
		}
	}

	@Override
	public void visitClass(PsiClass node) {
		for (JavaRecursiveElementVisitor visitor : visitors) {
			try {
				visitor.visitClass(node);
			} catch (ProcessCanceledException e) {
				//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
				//TODO delete block when minimum required version is 2024.2
				throw e;
			} catch (IndexNotReadyException | CancellationException e) {
				throw e;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error while visiting node with " + visitor.getClass().getName(), e);
			}
		}
	}

	@Override
	public void visitMethod(PsiMethod node) {
		for (JavaRecursiveElementVisitor visitor : visitors) {
			try {
				visitor.visitMethod(node);
			} catch (ProcessCanceledException e) {
				//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
				//TODO delete block when minimum required version is 2024.2
				throw e;
			} catch (IndexNotReadyException | CancellationException e) {
				throw e;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error while visiting node with " + visitor.getClass().getName(), e);
			}
		}
	}
}
