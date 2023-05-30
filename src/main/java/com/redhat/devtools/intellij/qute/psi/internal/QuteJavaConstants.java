/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal;

/**
 * Qute Java constants.
 * 
 * @author Angelo ZERR
 *
 */
public class QuteJavaConstants {

	public static final String JAVAX_INJECT_NAMED_ANNOTATION = "javax.inject.Named";

	public static final String LOCATION_ANNOTATION = "io.quarkus.qute.Location";

	public static final String TEMPLATE_CLASS = "io.quarkus.qute.Template";

	public static final String ENGINE_BUILDER_CLASS = "io.quarkus.qute.EngineBuilder";

	public static final String VALUE_ANNOTATION_NAME = "value";

	// @Decorator
	public static final String JAVAX_DECORATOR_ANNOTATION = "javax.decorator.Decorator";

	// @Vetoed
	public static final String JAVAX_INJECT_VETOED_ANNOTATION = "javax.enterprise.inject.Vetoed";

	// @CheckedTemplate

	public static final String CHECKED_TEMPLATE_ANNOTATION = "io.quarkus.qute.CheckedTemplate";

	public static final String OLD_CHECKED_TEMPLATE_ANNOTATION = "io.quarkus.qute.api.CheckedTemplate";

	public static final String CHECKED_TEMPLATE_ANNOTATION_IGNORE_FRAGMENTS = "ignoreFragments";

	// @TemplateExtension

	public static final String TEMPLATE_EXTENSION_ANNOTATION = "io.quarkus.qute.TemplateExtension";

	public static final String TEMPLATE_EXTENSION_ANNOTATION_NAMESPACE = "namespace";

	public static final String TEMPLATE_EXTENSION_ANNOTATION_MATCH_NAME = "matchName";

	// @TemplateData

	public static final String TEMPLATE_DATA_ANNOTATION = "io.quarkus.qute.TemplateData";

	public static final String TEMPLATE_DATA_ANNOTATION_IGNORE_SUPER_CLASSES = "ignoreSuperclasses";

	public static final String TEMPLATE_DATA_ANNOTATION_NAMESPACE = "namespace";

	public static final String TEMPLATE_DATA_ANNOTATION_TARGET = "target";

	public static final String TEMPLATE_DATA_ANNOTATION_IGNORE = "ignore";

	public static final String TEMPLATE_DATA_ANNOTATION_PROPERTIES = "properties";

	// @TemplateEnum

	public static final String TEMPLATE_ENUM_ANNOTATION = "io.quarkus.qute.TemplateEnum";

	// @TemplateGlobal

	public static final String TEMPLATE_GLOBAL_ANNOTATION = "io.quarkus.qute.TemplateGlobal";

	public static final String TEMPLATE_GLOBAL_ANNOTATION_NAME = "name";

	// @io.quarkus.runtime.annotations.RegisterForReflection

	public static final String REGISTER_FOR_REFLECTION_ANNOTATION = "io.quarkus.runtime.annotations.RegisterForReflection";

	public static final String REGISTER_FOR_REFLECTION_ANNOTATION_FIELDS = "fields";

	public static final String REGISTER_FOR_REFLECTION_ANNOTATION_METHODS = "methods";

	public static final String REGISTER_FOR_REFLECTION_ANNOTATION_TARGETS = "targets";
}
