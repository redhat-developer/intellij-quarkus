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
    public static final String CONFIG_PROPERTY_ANNOTATION = "org.eclipse.microprofile.config.inject.ConfigProperty";

    public static final String QUARKUS_PREFIX = "quarkus.";
    public static final String QUARKUS_JAVADOC_PROPERTIES = "quarkus-javadoc.properties";
    public static final String QUARKUS_EXTENSION_PROPERTIES = "META-INF/quarkus-extension.properties";
    public static final String QUARKUS_DEPLOYMENT_PROPERTY_NAME = "deployment-artifact";

    public static final String QUARKUS_DEPLOYMENT_LIBRARY_NAME = "Quarkus (deployment)";

    public static final String LSP_PLUGIN_ID = "com.github.gtache.lsp";
    public static final String DISPLAY_CHECK_NOTIFACTION_PROPERTY_NAME = QuarkusConstants.class.getPackage().getName() + ".displayCheckNotification";
    public static final String NOTIFICATION_GROUP = "Quarkus";

    public static final String CODE_QUARKUS_IO_CLIENT_NAME_HEADER_NAME = "Client-Name";
    public static final String CODE_QUARKUS_IO_CLIENT_NAME_HEADER_VALUE = "IntelliJ Quarkus";
    public static final String CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_NAME = "Client-Contact-Email";
    public static final String CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_VALUE = "tools@jboss.org";
    public static final String QUARKUS_CODE_URL = "https://code.quarkus.io";
}
