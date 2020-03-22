/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.providers;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiManager;
import com.redhat.devtools.intellij.quarkus.search.IPropertiesCollector;
import com.redhat.devtools.intellij.quarkus.search.PsiTypeUtils;
import com.redhat.devtools.intellij.quarkus.search.SearchContext;

import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileMetricsConstants.APPLICATION_NAME_VARIABLE;
import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileMetricsConstants.GLOBAL_TAGS_VARIABLE;
import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileMetricsConstants.METRIC_ID;

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

	@Override
	protected boolean isAdaptedFor(SearchContext context) {
		Module javaProject = context.getModule();
		return (PsiTypeUtils.findType(javaProject, METRIC_ID) != null);
	}

	@Override
	protected void collectStaticProperties(SearchContext context) {
		IPropertiesCollector collector = context.getCollector();
		String docs = "List of tag values.\r\n"
				+ "Tag values set through `mp.metrics.tags` MUST escape equal symbols `=` and commas `,` with a backslash `\\`.";
		super.addItemMetadata(collector, GLOBAL_TAGS_VARIABLE, "java.util.Optional<java.lang.String>", docs, null, null,
				null, null, null, false);

		docs = "The app name.";
		super.addItemMetadata(collector, APPLICATION_NAME_VARIABLE, "java.util.Optional<java.lang.String>", docs, null,
				null, null, null, null, false);
	}

}
