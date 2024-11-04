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
package com.redhat.devtools.intellij.qute.psi.internal.extensions.roq;

import java.util.Arrays;


import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelTemplate;
import com.redhat.qute.commons.datamodel.DataModelTemplateMatcher;

/**
 * Inject 'site' and 'page' as data model parameters for all Qute templates
 * which belong to a Roq application.
 */
public class RoqDataModelProvider extends AbstractDataModelProvider {

	@Override
	public void beginSearch(SearchContext context, ProgressIndicator monitor) {
		if (!RoqUtils.isRoqProject(context.getJavaProject())) {
			// It is not a Roq application, don't inject site and page.
			return;
		}
		//quarkus-roq-frontmatter

		DataModelTemplate<DataModelParameter> roqTemplate = new DataModelTemplate<DataModelParameter>();
		roqTemplate.setTemplateMatcher(new DataModelTemplateMatcher(Arrays.asList("**/**")));

		// site
		DataModelParameter site = new DataModelParameter();
		site.setKey("site");
		site.setSourceType(RoqJavaConstants.SITE_CLASS);
		roqTemplate.addParameter(site);

		// page
		DataModelParameter page = new DataModelParameter();
		page.setKey("page");
		page.setSourceType(RoqJavaConstants.PAGE_CLASS);
		roqTemplate.addParameter(page);

		context.getDataModelProject().getTemplates().add(roqTemplate);
	}

	@Override
	public void collectDataModel(Object match, SearchContext context, ProgressIndicator monitor) {
		// Do nothing
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
