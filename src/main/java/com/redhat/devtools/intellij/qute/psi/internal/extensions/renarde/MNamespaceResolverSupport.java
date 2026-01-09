/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.extensions.renarde;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.DirectClassInheritorsSearch;
import com.intellij.psi.util.PsiClassUtil;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;

import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverKind;

import static com.redhat.devtools.intellij.qute.psi.internal.extensions.renarde.RenardeJavaConstants.RENARDE_CONTROLLER_TYPE;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.JAVA_LANG_OBJECT_TYPE;

/**
 * m renarde support.
 * 
 * @author Angelo ZERR
 * 
 * @see <a href=
 *      "https://docs.quarkiverse.io/quarkus-renarde/dev/advanced.html#localisation">Localisation
 *      / Internationalisation</a>
 *
 */
public class MNamespaceResolverSupport extends AbstractDataModelProvider {

	@Override
	protected boolean isNamespaceAvailable(String namespace, SearchContext context, ProgressIndicator monitor) {
		// m namespace is available only for renarde project
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, RENARDE_CONTROLLER_TYPE) != null;
	}

	@Override
	public void collectDataModel(Object match, SearchContext context, ProgressIndicator monitor) {

	}

	@Override
	protected String[] getPatterns() {
		return null;
	}

	@Override
	protected Query<? extends Object> createSearchPattern(SearchContext context, String pattern) {
		return null;
	}
}
