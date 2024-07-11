/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.template.datamodel;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiClass;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract class for data model provider based on class type search which
 * implements some interfaces.
 *
 * @author Angelo ZERR
 *
 */
public abstract class AbstractInterfaceImplementationDataModelProvider extends AbstractDataModelProvider {

	private static final Logger LOGGER = Logger
			.getLogger(AbstractInterfaceImplementationDataModelProvider.class.getName());

	@Override
	protected String[] getPatterns() {
		return getInterfaceNames();
	}

	/**
	 * Returns the interface names to search.
	 *
	 * @return the interface names to search.
	 */
	protected abstract String[] getInterfaceNames();

	@Override
	protected Query<? extends Object> createSearchPattern(SearchContext context, String interfaceName) {
		return createInterfaceImplementationSearchPattern(context, interfaceName);
	}

	@Override
	public void collectDataModel(Object match, SearchContext context, ProgressIndicator monitor) {
		Object element = match;
		if (element instanceof PsiClass type) {
			try {
				if (isApplicable(type)) {
					processType(type, context, monitor);
				}
			} catch (Exception e) {
				if (LOGGER.isLoggable(Level.SEVERE)) {
					LOGGER.log(Level.SEVERE,
							"Cannot collect Qute data model for the type '" + type.getQualifiedName() + "'.", e);
				}
			}
		}
	}

	private boolean isApplicable(PsiClass type) {
		PsiClass @NotNull [] superInterfaceNames = type.getInterfaces();
		if (superInterfaceNames == null || superInterfaceNames.length == 0) {
			return false;
		}
		for (String interfaceName : getInterfaceNames()) {
			for (PsiClass superInterfaceName : superInterfaceNames) {
				if (interfaceName.equals(superInterfaceName.getQualifiedName())) {
					return true;
				}
			}
		}
		return false;
	}

	protected abstract void processType(PsiClass recordElement, SearchContext context, ProgressIndicator monitor);

}
