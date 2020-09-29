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

import static com.redhat.devtools.intellij.quarkus.search.internal.health.MicroProfileHealthConstants.LIVENESS_ANNOTATION;

/**
 * Properties provider that provides static MicroProfile Health properties
 *
 * @author Ryan Zegray
 *
 * @see <a href="https://github.com/eclipse/microprofile-health/blob/master/spec/src/main/asciidoc/protocol-wireformat.adoc">https://github.com/eclipse/microprofile-health/blob/master/spec/src/main/asciidoc/protocol-wireformat.adoc</a>
 *
 */
public class MicroProfileHealthProvider extends AbstractStaticPropertiesProvider {

    public MicroProfileHealthProvider() {
        super("/static-properties/mp-health-metadata.json");
    }

    @Override
    protected boolean isAdaptedFor(SearchContext context) {
        // Check if MicroProfile Health exists in classpath
        Module javaProject = context.getModule();
        return (PsiTypeUtils.findType(javaProject, LIVENESS_ANNOTATION) != null);
    }

}