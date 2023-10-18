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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;

import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ITypeResolver;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import com.redhat.qute.commons.QuteProjectScope;
import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelProject;
import com.redhat.qute.commons.datamodel.DataModelTemplate;

import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.getRelativeTemplateBaseDir;

/**
 * The search context used to collect properties.
 *
 * @author Angelo ZERR
 *
 */
public class SearchContext extends BaseContext {
	private final DataModelProject<DataModelTemplate<DataModelParameter>> dataModelProject;
	private final String relativeTemplateBaseDir;

	private Map<PsiClass, ITypeResolver> typeResolvers;

	private final IPsiUtils utils;

	public SearchContext(Module javaProject,
						 DataModelProject<DataModelTemplate<DataModelParameter>> dataModelProject, IPsiUtils utils,
						 List<QuteProjectScope> scopes) {
		super(javaProject, scopes);
		this.dataModelProject = dataModelProject;
		this.utils = utils;
		relativeTemplateBaseDir = PsiQuteProjectUtils.getRelativeTemplateBaseDir(javaProject);
	}

	public DataModelProject<DataModelTemplate<DataModelParameter>> getDataModelProject() {
		return dataModelProject;
	}

	/**
	 * Returns the JDT utilities.
	 *
	 * @return the JDT utilities.
	 */
	public IPsiUtils getUtils() {
		return utils;
	}

	/**
	 * Returns the {@link ITypeResolver} of the given Java type <code>type</code>.
	 *
	 * @param type the Java type.
	 *
	 * @return the {@link ITypeResolver} of the given Java type <code>type</code>.
	 */
	public ITypeResolver getTypeResolver(PsiClass type) {
		if (typeResolvers == null) {
			typeResolvers = new HashMap<>();
		}
		ITypeResolver typeResolver = typeResolvers.get(type);
		if (typeResolver == null) {
			typeResolver = QuteSupportForTemplate.createTypeResolver(type, utils.getModule());
			typeResolvers.put(type, typeResolver);
		}
		return typeResolver;
	}

	public String getRelativeTemplateBaseDir() {
		return relativeTemplateBaseDir;
	}
}
