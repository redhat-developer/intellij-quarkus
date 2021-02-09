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
package com.redhat.devtools.intellij.quarkus.search.providers;

import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileOpenAPIConstants.OPEN_API_CONFIG;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.quarkus.search.SearchContext;
import com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils;

/**
 * Properties provider that provides static MicroProfile OpenAPI properties
 * 
 * @author David Kwon
 * 
 * @see <a href="https://github.com/eclipse/microprofile-open-api/blob/master/spec/src/main/asciidoc/microprofile-openapi-spec.adoc#311-core-configurations"></a>https://github.com/eclipse/microprofile-open-api/blob/master/spec/src/main/asciidoc/microprofile-openapi-spec.adoc#311-core-configurations</a>
 *
 */
public class MicroProfileOpenAPIProvider extends AbstractStaticPropertiesProvider {

	public MicroProfileOpenAPIProvider() {
		super("/static-properties/mp-openapi-metadata.json");
	}

	@Override
	protected boolean isAdaptedFor(SearchContext context) {
		Module javaProject = context.getModule();
		return (PsiTypeUtils.findType(javaProject, OPEN_API_CONFIG) != null);
	}
}
