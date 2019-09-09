package com.redhat.devtools.intellij.quarkus.lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class ApplicationPropertiesFileType extends LanguageFileType {
    public static final String EXTENSION = "properties";

    public static final ApplicationPropertiesFileType INSTANCE = new ApplicationPropertiesFileType();

    private ApplicationPropertiesFileType() {
        super(ApplicationPropertiesLanguage.INSTANCE);
    }
    @NotNull
    @Override
    public String getName() {
        return "Quarkus properties";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Quarkus properties file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return ApplicationPropertiesFileType.EXTENSION;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return null;
    }
}
