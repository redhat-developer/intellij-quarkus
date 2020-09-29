/*******************************************************************************
* Copyright (c) 2020 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/

package com.redhat.devtools.intellij.quarkus.search.providers;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.quarkus.search.SearchContext;
import com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils;

/**
 * Properties provider that provides static MicroProfile GraphQL properties
 * 
 * @author Kathryn Kodama
 * 
 * @see <a href="</a>https://github.com/eclipse/microprofile-graphql/blob/905456693d9bfb1f78efe0d7e614db6fde3324b2/server/spec/src/main/asciidoc/errorhandling.asciidoc">https://github.com/eclipse/microprofile-graphql/blob/905456693d9bfb1f78efe0d7e614db6fde3324b2/server/spec/src/main/asciidoc/errorhandling.asciidoc</a>
 *
 */
public class MicroProfileGraphQLProvider extends AbstractStaticPropertiesProvider {

    public MicroProfileGraphQLProvider() {
        super("/static-properties/mp-graphql-metadata.json");
    }

    @Override
    protected boolean isAdaptedFor(SearchContext context) {
        // Check if MicroProfile GraphQL exists in classpath
        Module javaProject = context.getModule();
        return (PsiTypeUtils.findType(javaProject, MicroProfileGraphQLConstants.GRAPHQL_NAME_TAG) != null);
    }

}