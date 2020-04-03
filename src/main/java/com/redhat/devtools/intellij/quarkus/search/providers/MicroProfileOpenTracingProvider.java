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

import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileOpenTracingConstants.OPERATION_NAME_PROVIDER;
import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileOpenTracingConstants.SKIP_PATTERN;
import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileOpenTracingConstants.TRACED_ANNOTATION;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.quarkus.search.IPropertiesCollector;
import com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.quarkus.search.SearchContext;

import com.redhat.microprofile.commons.metadata.ItemHint;
import com.redhat.microprofile.commons.metadata.ItemHint.ValueHint;

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

	@Override
	protected boolean isAdaptedFor(SearchContext context) {
		Module javaProject = context.getModule();
		return (PsiTypeUtils.findType(javaProject, TRACED_ANNOTATION) != null);
	}

	@Override
	protected void collectStaticProperties(SearchContext context) {
		IPropertiesCollector collector = context.getCollector();
		String docs = "Specifies a skip pattern to avoid tracing of selected REST endpoints.";
		super.addItemMetadata(collector, SKIP_PATTERN, "java.util.Optional<java.util.regex.Pattern>", docs, null, null,
				null, null, null, false);

		docs = "Specifies operation name provider for server spans. Possible values are `http-path` and `class-method`.";
		super.addItemMetadata(collector, OPERATION_NAME_PROVIDER, "\"http-path\" or \"class-method\"", docs, null,
				null, null, "class-method", null, false);
		
		ItemHint itemHint = collector.getItemHint(OPERATION_NAME_PROVIDER);
		addHint(itemHint, "class-method", "The provider for the default operation name.");
		addHint(itemHint, "http-path",
				"The operation name has the following form `<HTTP method>:<@Path value of endpoint’s class>/<@Path value of endpoint’s method>`. "
				+ "For example if the class is annotated with `@Path(\"service\")` and method `@Path(\"endpoint/{id: \\\\d+}\")` "
				+ "then the operation name is `GET:/service/endpoint/{id: \\\\d+}`.");
	}
	
	private void addHint(ItemHint itemHint, String value, String description) {
		ValueHint hint = new ValueHint();
		hint.setValue(value);
		hint.setDescription(description);
		itemHint.getValues().add(hint);
	}
}