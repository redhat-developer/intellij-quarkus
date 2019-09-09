package com.redhat.devtools.intellij.quarkus;

import com.intellij.openapi.util.Key;
import com.redhat.devtools.intellij.quarkus.module.QuarkusModel;

public class QuarkusConstants {
    public final static Key<String> WIZARD_GROUPID_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".groupId");
    public final static Key<String> WIZARD_ARTIFACTID_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".artifactId");
    public final static Key<String> WIZARD_VERSION_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".version");
    public final static Key<String> WIZARD_CLASSNAME_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".className");
    public final static Key<String> WIZARD_PATH_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".path");
    public final static Key<QuarkusModel> WIZARD_MODEL_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".model");
    public final static Key<String> WIZARD_ENDPOINT_URL_KEY = Key.create(QuarkusConstants.class.getPackage().getName() + ".endpointURL");
    public static final String CONFIG_ROOT_ANNOTATION = "io.quarkus.runtime.annotations.ConfigRoot";
    public static final String CONFIG_ITEM_ANNOTATION = "io.quarkus.runtime.annotations.ConfigItem";
    public static final String QUARKUS_PREFIX = "quarkus.";
    public static final String CONFIG_GROUP_ANNOTATION = "io.quarkus.runtime.annotations.ConfigGroup";
    public static final String QUARKUS_JAVADOC_PROPERTIES = "quarkus-javadoc.properties";
}
