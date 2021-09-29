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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.properties;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractStaticPropertiesProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants.METRIC_ID;

/**
 * Properties provider that provides static MicroProfile Metrics properties
 * 
 * @author David Kwon
 * 
 * @see <a ref="https://github.com/eclipse/microprofile-metrics/blob/dc94b84ec90dd4a0bc983336e4273247a4e415cc/spec/src/main/asciidoc/architecture.adoc">https://github.com/eclipse/microprofile-metrics/blob/dc94b84ec90dd4a0bc983336e4273247a4e415cc/spec/src/main/asciidoc/architecture.adoc</a>
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/metrics/properties/MicroProfileMetricsProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/metrics/properties/MicroProfileMetricsProvider.java</a>
 *
 */
public class MicroProfileMetricsProvider extends AbstractStaticPropertiesProvider {

	public MicroProfileMetricsProvider() {
		super("/static-properties/mp-metrics-metadata.json");
	}

	@Override
	protected boolean isAdaptedFor(SearchContext context) {
		Module javaProject = context.getJavaProject();
		return (PsiTypeUtils.findType(javaProject, METRIC_ID) != null);
	}
}
