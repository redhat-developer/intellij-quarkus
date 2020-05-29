/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus;

import com.intellij.openapi.util.Key;
import com.redhat.devtools.intellij.quarkus.module.QuarkusModel;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;

public class QuarkusConstants {
    public final static Key<ToolDelegate> WIZARD_TOOL_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".tool");
    public final static Key<String> WIZARD_GROUPID_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".groupId");
    public final static Key<String> WIZARD_ARTIFACTID_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".artifactId");
    public final static Key<String> WIZARD_VERSION_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".version");
    public final static Key<String> WIZARD_CLASSNAME_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".className");
    public final static Key<String> WIZARD_PATH_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".path");
    public final static Key<QuarkusModel> WIZARD_MODEL_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".model");
    public final static Key<String> WIZARD_ENDPOINT_URL_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".endpointURL");

    public static final String CONFIG_ROOT_ANNOTATION = "io.quarkus.runtime.annotations.ConfigRoot";
    public static final String CONFIG_GROUP_ANNOTATION = "io.quarkus.runtime.annotations.ConfigGroup";
    public static final String CONFIG_ITEM_ANNOTATION = "io.quarkus.runtime.annotations.ConfigItem";
    public static final String CONFIG_PROPERTIES_ANNOTATION = "io.quarkus.arc.config.ConfigProperties";

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


    public static final String QUARKUS_PREFIX = "quarkus";
    public static final String QUARKUS_JAVADOC_PROPERTIES_FILE = "quarkus-javadoc.properties";
    public static final String QUARKUS_EXTENSION_PROPERTIES = "META-INF/quarkus-extension.properties";
    public static final String QUARKUS_DEPLOYMENT_PROPERTY_NAME = "deployment-artifact";

    public static final String QUARKUS_DEPLOYMENT_LIBRARY_NAME = "Quarkus (deployment)";

    public static final String LSP_PLUGIN_ID = "com.github.gtache.lsp";
    public static final String DISPLAY_CHECK_NOTIFACTION_PROPERTY_NAME = QuarkusConstants.class.getPackage().getName() + ".displayCheckNotification";
    public static final String NOTIFICATION_GROUP = "Quarkus Tools";

    public static final String CODE_QUARKUS_IO_CLIENT_NAME_HEADER_NAME = "Client-Name";
    public static final String CODE_QUARKUS_IO_CLIENT_NAME_HEADER_VALUE = "IntelliJ Quarkus Tools";
    public static final String CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_NAME = "Client-Contact-Email";
    public static final String CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_VALUE = "tools@jboss.org";

    public static final String QUARKUS_CODE_URL_PROPERTY_NAME = "com.redhat.devtools.intellij.quarkus.code.url";
    public static final String QUARKUS_CODE_URL_PRODUCTION = "https://code.quarkus.io";
    public static final String QUARKUS_CODE_URL_TEST = "https://stage.code.quarkus.io";
    public static final String QUARKUS_CODE_URL = System.getProperty(QUARKUS_CODE_URL_PROPERTY_NAME, QUARKUS_CODE_URL_PRODUCTION);
}
