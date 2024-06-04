/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.qute.psi.template.datamodel;

import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;

/**
 * Abstract class for data model provider based on field declaration type
 * (class, interface, annotation type, etc) search.
 *
 * @author Angelo ZERR
 *
 */
public abstract class AbstractFieldDeclarationTypeReferenceDataModelProvider extends AbstractDataModelProvider {

	private static final Logger LOGGER = Logger
			.getLogger(AbstractFieldDeclarationTypeReferenceDataModelProvider.class.getName());

	@Override
	protected String[] getPatterns() {
		return getTypeNames();
	}

	/**
	 * Returns the type names to search.
	 *
	 * @return the type names to search.
	 */
	protected abstract String[] getTypeNames();

	@Override
	protected Query<? extends Object> createSearchPattern(SearchContext context, String className) {
		return createFieldDeclarationTypeReferenceSearchPattern(context, className);
	}

	@Override
	public void collectDataModel(Object match, SearchContext context, ProgressIndicator monitor) {
		Object element = match;
		if (element instanceof PsiReference) {
			PsiField field = PsiTreeUtil.getParentOfType(((PsiReference) element).getElement(), PsiField.class);
			if (field == null || !isApplicable(field)) {
				return;
			}
			try {
				// Collect properties from the class name and stop the loop.
				processField(field, context, monitor);
			} catch (ProcessCanceledException e) {
				//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
				//TODO delete block when minimum required version is 2024.2
				throw e;
			} catch (IndexNotReadyException | CancellationException e) {
				throw e;
			} catch (Exception e) {
				if (LOGGER.isLoggable(Level.WARNING)) {
					LOGGER.log(
							Level.WARNING, "Cannot collect Qute data model for the field '"
									+ field.getContainingClass().getQualifiedName() + "#" + field.getName() + "'.",
							e);
				}
			}
		}
	}

	private boolean isApplicable(PsiField field) {
		String fieldTypeName = PsiTypeUtils.getResolvedTypeName(field);
		for (String typeName : getTypeNames()) {
			if (typeName.endsWith(fieldTypeName)) {
				return true;
			}
		}
		return false;
	}

	protected abstract void processField(PsiField field, SearchContext context, ProgressIndicator monitor);
}
