/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus;

import com.intellij.openapi.util.Key;
import com.redhat.devtools.intellij.quarkus.projectWizard.QuarkusExtensionsModel;
import com.redhat.devtools.intellij.quarkus.projectWizard.QuarkusModel;
import com.redhat.devtools.intellij.quarkus.buildtool.BuildToolDelegate;
import org.jetbrains.jps.model.java.JdkVersionDetector;

public class QuarkusConstants {
    public final static Key<BuildToolDelegate> WIZARD_TOOL_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".tool");
    public final static Key<Boolean> WIZARD_EXAMPLE_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".example");
    public final static Key<String> WIZARD_GROUPID_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".groupId");
    public final static Key<String> WIZARD_ARTIFACTID_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".artifactId");
    public final static Key<String> WIZARD_VERSION_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".version");
    public final static Key<String> WIZARD_CLASSNAME_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".className");
    public final static Key<String> WIZARD_PATH_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".path");
    public final static Key<QuarkusExtensionsModel> WIZARD_EXTENSIONS_MODEL_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".model");
    public final static Key<Integer> WIZARD_JAVA_VERSION_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".javaVersion");
    public final static Key<String> WIZARD_ENDPOINT_URL_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".endpointURL");
    public final static Key<QuarkusModel> WIZARD_QUARKUS_STREAMS = Key.create(QuarkusConstants.class.getPackage().getName() + ".streams");
    public final static Key<JdkVersionDetector.JdkVersionInfo> WIZARD_JDK_INFO_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".jdkVersionInfo");

    public static final String CONFIG_ROOT_ANNOTATION = "io.quarkus.runtime.annotations.ConfigRoot";
    public static final String CONFIG_GROUP_ANNOTATION = "io.quarkus.runtime.annotations.ConfigGroup";
    public static final String CONFIG_ITEM_ANNOTATION = "io.quarkus.runtime.annotations.ConfigItem";
    public static final String CONFIG_PROPERTIES_ANNOTATION = "io.quarkus.arc.config.ConfigProperties";
    public static final String QUARKUS_DIAGNOSTIC_SOURCE = "quarkus";


    /**
     * As ConfigProperties is not part anymore of Quarkus since 3.1.2.Final,
     * we defined this constant to be compatible with project based on previous Quarkus
     * releases
     */
    public static final String CONFIG_PROPERTIES_UNSET_PREFIX = "<< unset >>";

    public static final String CONFIG_ANNOTATION_NAME = "name";
    public static final String CONFIG_ROOT_ANNOTATION_PHASE = "phase";
    public static final String CONFIG_ITEM_ANNOTATION_DEFAULT_VALUE = "defaultValue";

    public static final String CONFIG_PROPERTY_ANNOTATION = "org.eclipse.microprofile.config.inject.ConfigProperty";
    public static final String CONFIG_PROPERTY_ANNOTATION_NAME = "name";
    public static final String CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE = "defaultValue";

    /**
     * The Quarkus @ConfigProperties annotation
     */
    public static final String CONFIG_PROPERTIES_ANNOTATION_NAMING_STRATEGY = "namingStrategy";

    public static final String CONFIG_PROPERTIES_NAMING_STRATEGY_ENUM = CONFIG_PROPERTIES_ANNOTATION
            + ".NamingStrategy";

    public static final String NAMING_STRATEGY_PREFIX = "NamingStrategy.";

    public static final String CONFIG_PROPERTIES_NAMING_STRATEGY_ENUM_FROM_CONFIG = NAMING_STRATEGY_PREFIX
            + "FROM_CONFIG";

    public static final String CONFIG_PROPERTIES_NAMING_STRATEGY_ENUM_VERBATIM = NAMING_STRATEGY_PREFIX + "VERBATIM";

    public static final String CONFIG_PROPERTIES_NAMING_STRATEGY_ENUM_KEBAB_CASE = NAMING_STRATEGY_PREFIX
            + "KEBAB_CASE";

    public static final String QUARKUS_ARC_CONFIG_PROPERTIES_DEFAULT_NAMING_STRATEGY = "quarkus.arc.config-properties-default-naming-strategy";

    public static final String QUARKUS_DEPLOYMENT_BUILDSTEP_ANNOTATION = "io.quarkus.deployment.annotations.BuildStep";

    public static final String QUARKUS_SCHEDULED_ANNOTATION = "io.quarkus.scheduler.Scheduled";

    public static final String QUARKUS_CORE_PREFIX = "io.quarkus:quarkus-core:";

    public static final String QUARKUS_VERTX_HTTP_PREFIX = "io.quarkus:quarkus-vertx-http:";

    public static final String QUARKUS_PREFIX = "quarkus";
    public static final String QUARKUS_JAVADOC_PROPERTIES_FILE = "quarkus-javadoc.properties";
    public static final String QUARKUS_EXTENSION_PROPERTIES = "META-INF/quarkus-extension.properties";
    public static final String QUARKUS_DEPLOYMENT_PROPERTY_NAME = "deployment-artifact";
    public static final String QUARKUS_DEPLOYMENT_LIBRARY_NAME = "Quarkus (deployment)";

    public static final Integer QUARKUS_DEPLOYMENT_LIBRARY_VERSION = 1;

    public static final String DISPLAY_CHECK_NOTIFACTION_PROPERTY_NAME = QuarkusConstants.class.getPackage().getName() + ".displayCheckNotification";
    public static final String NOTIFICATION_GROUP = "Quarkus Tools";

    /*
     * Parameter names for the download request. See
     * http://editor.swagger.io/?url=https://code.quarkus.io/openapi for reference.
     */
    public static final String CODE_TOOL_PARAMETER_NAME = "buildTool";

    public static final String CODE_GROUP_ID_PARAMETER_NAME = "groupId";

    public static final String CODE_ARTIFACT_ID_PARAMETER_NAME = "artifactId";

    public static final String CODE_VERSION_PARAMETER_NAME = "version";

    public static final String CODE_CLASSNAME_PARAMETER_NAME = "className";

    public static final String CODE_JAVA_VERSION_PARAMETER_NAME = "javaVersion";

    public static final String CODE_PATH_PARAMETER_NAME = "path";

    public static final String CODE_NO_EXAMPLES_NAME = "noExamples";
    public static final Boolean CODE_NO_EXAMPLES_DEFAULT = Boolean.TRUE;

    public static final String CODE_EXTENSIONS_PARAMETER_NAME = "extensions";
    public static final String CODE_EXTENSIONS_SHORT_PARAMETER_NAME = "s";

    public static final String CODE_STREAM_PARAMETER_NAME = "streamKey";

    public static final String PLATFORM_ONLY_PARAMETER = "platformOnly";

    public static final String CODE_QUARKUS_IO_CLIENT_NAME_HEADER_NAME = "Client-Name";
    public static final String CODE_QUARKUS_IO_CLIENT_NAME_HEADER_VALUE = "IntelliJ Quarkus Tools";
    public static final String CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_NAME = "Client-Contact-Email";
    public static final String CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_VALUE = "tools@jboss.org";

    public static final String QUARKUS_CODE_URL_PROPERTY_NAME = "com.redhat.devtools.intellij.quarkus.code.url";
    public static final String QUARKUS_CODE_URL_PRODUCTION = "https://code.quarkus.io";
    public static final String QUARKUS_CODE_URL_TEST = "https://stage.code.quarkus.io";
    public static final String QUARKUS_CODE_URL = System.getProperty(QUARKUS_CODE_URL_PROPERTY_NAME, QUARKUS_CODE_URL_PRODUCTION);

    public static final String QUARKUS_RUNTIME_CLASS_NAME = "io.quarkus.runtime.LaunchMode";
    public static final String QUARKUS_BUILD_ITEM_CLASS_NAME = "io.quarkus.builder.item.BuildItem";
    public static final String QUARKUS_RUN_CONTEXT_KEY = QuarkusConstants.class.getName() + ".quarkusContext";
}
