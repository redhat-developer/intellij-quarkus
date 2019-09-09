package com.redhat.devtools.intellij.quarkus.lang;

import com.intellij.lang.Language;

public class ApplicationPropertiesLanguage extends Language {
    public static final ApplicationPropertiesLanguage INSTANCE = new ApplicationPropertiesLanguage();

    protected ApplicationPropertiesLanguage() {
        super("Quarkus properties", "text/properties");
    }
}
