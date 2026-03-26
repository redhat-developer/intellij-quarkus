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


import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;

import static com.redhat.devtools.intellij.qute.psi.internal.extensions.renarde.RenardeUtils.isRenardeProject;

/**
 * m renarde support.
 *
 * @author Angelo ZERR
 * @see <a href=
 * "https://docs.quarkiverse.io/quarkus-renarde/dev/advanced.html#localisation">Localisation
 * / Internationalisation</a>
 *
 */
public class MNamespaceResolverSupport extends AbstractDataModelProvider {

    @Override
    protected boolean isNamespaceAvailable(String namespace, SearchContext context, ProgressIndicator monitor) {
        // m namespace is available only for renarde project
        return isRenardeProject(context.getJavaProject());
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
