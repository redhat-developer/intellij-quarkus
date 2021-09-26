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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.opentracing.properties;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractStaticPropertiesProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.opentracing.MicroProfileOpenTracingConstants.TRACED_ANNOTATION;

/**
 * Properties provider that provides static MicroProfile Metrics properties
 * 
 * @author David Kwon
 * 
 * @see <a href="https://github.com/eclipse/microprofile-opentracing/blob/master/spec/src/main/asciidoc/configuration.asciidoc">https://github.com/eclipse/microprofile-opentracing/blob/master/spec/src/main/asciidoc/configuration.asciidoc</a>
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/metrics/properties/MicroProfileMetricsProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/opentracing/properties/MicroProfileOpenTracingProvider.java</a>
 *
 */
public class MicroProfileOpenTracingProvider extends AbstractStaticPropertiesProvider {

	public MicroProfileOpenTracingProvider() {
		super("/static-properties/mp-opentracing-metadata.json");
	}

	@Override
	protected boolean isAdaptedFor(SearchContext context) {
		Module javaProject = context.getModule();
		return (PsiTypeUtils.findType(javaProject, TRACED_ANNOTATION) != null);
	}
}