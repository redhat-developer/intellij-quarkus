/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.contextpropagation.properties;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractStaticPropertiesProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.contextpropagation.MicroProfileContextPropagationConstants.CONTEXT_PROPAGATION_ANNOTATION;

public class MicroProfileContextPropagationProvider extends AbstractStaticPropertiesProvider {
    public MicroProfileContextPropagationProvider() {
        super("/static-properties/mp-context-propagation-metadata.json");
    }

    @Override
    protected boolean isAdaptedFor(SearchContext context) {
        // Check if MicroProfile context propagation exists in classpath
        Module javaProject = context.getModule();
        return (PsiTypeUtils.findType(javaProject, CONTEXT_PROPAGATION_ANNOTATION) != null);
    }
}
