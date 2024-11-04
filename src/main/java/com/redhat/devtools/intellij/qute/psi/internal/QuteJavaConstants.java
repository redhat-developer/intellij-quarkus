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

import java.util.Collection;
import java.util.List;

/**
 * Qute Java constants.
 *
 * @author Angelo ZERR
 */
public class QuteJavaConstants {

    public static final String JAVA_LANG_OBJECT_TYPE = "java.lang.Object";

    public static final String JAVAX_INJECT_NAMED_ANNOTATION = "javax.inject.Named";

    public static final String JAKARTA_INJECT_NAMED_ANNOTATION = "jakarta.inject.Named";

    public static final String LOCATION_ANNOTATION = "io.quarkus.qute.Location";

    public static final String TEMPLATE_CLASS = "io.quarkus.qute.Template";

    public static final String TEMPLATE_INSTANCE_INTERFACE = "io.quarkus.qute.TemplateInstance";

    public static Collection<String> QUTE_MAVEN_COORDS = List.of("io.quarkus:quarkus-qute", "io.quarkus.qute:qute-core");

    public static final String ENGINE_BUILDER_CLASS = "io.quarkus.qute.EngineBuilder";

    public static final String VALUE_ANNOTATION_NAME = "value";

    // @Decorator
    public static final String JAVAX_DECORATOR_ANNOTATION = "javax.decorator.Decorator";

    public static final String JAKARTA_DECORATOR_ANNOTATION = "jakarta.decorator.Decorator";

    // @Vetoed
    public static final String JAVAX_INJECT_VETOED_ANNOTATION = "javax.enterprise.inject.Vetoed";

    public static final String JAKARTA_INJECT_VETOED_ANNOTATION = "jakarta.enterprise.inject.Vetoed";

    // @CheckedTemplate

    public static final String CHECKED_TEMPLATE_ANNOTATION = "io.quarkus.qute.CheckedTemplate";

    public static final String OLD_CHECKED_TEMPLATE_ANNOTATION = "io.quarkus.qute.api.CheckedTemplate";

    public static final String CHECKED_TEMPLATE_ANNOTATION_IGNORE_FRAGMENTS = "ignoreFragments";
    public static final String CHECKED_TEMPLATE_ANNOTATION_BASE_PATH = "basePath";
    public static final String CHECKED_TEMPLATE_ANNOTATION_DEFAULT_NAME = "defaultName";
    public static final String CHECKED_TEMPLATE_ANNOTATION_DEFAULT_NAME_HYPHENATED_ELEMENT_NAME = "<<hyphenated element name>>";
    public static final String CHECKED_TEMPLATE_ANNOTATION_DEFAULT_NAME_UNDERSCORED_ELEMENT_NAME = "<<underscored element name>>";
    
    // @TemplateExtension

    public static final String TEMPLATE_EXTENSION_ANNOTATION = "io.quarkus.qute.TemplateExtension";

    public static final String TEMPLATE_EXTENSION_ANNOTATION_NAMESPACE = "namespace";

    public static final String TEMPLATE_EXTENSION_ANNOTATION_MATCH_NAME = "matchName";

    public static final String TEMPLATE_EXTENSION_ANNOTATION_MATCH_NAMES = "matchNames";

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

    // @Message
    public static final String MESSAGE_BUNDLE_ANNOTATION = "io.quarkus.qute.i18n.MessageBundle";
    public static final String MESSAGE_BUNDLE_ANNOTATION_LOCALE = "locale";
    public static final String MESSAGE_ANNOTATION = "io.quarkus.qute.i18n.Message";
    public static final String MESSAGE_ANNOTATION_KEY = "key";
}
