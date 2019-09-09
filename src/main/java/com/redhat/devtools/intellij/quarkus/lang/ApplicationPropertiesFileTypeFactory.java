package com.redhat.devtools.intellij.quarkus.lang;

import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.FileTypeConsumer;
import org.jetbrains.annotations.NotNull;

public class ApplicationPropertiesFileTypeFactory extends com.intellij.openapi.fileTypes.FileTypeFactory {
    @Override
    public void createFileTypes(@NotNull FileTypeConsumer consumer) {
        consumer.consume(ApplicationPropertiesFileType.INSTANCE, new FileNameMatcher() {
            @Override
            public boolean accept(@NotNull String fileName) {
                return fileName.endsWith("application.properties");
            }

            @NotNull
            @Override
            public String getPresentableString() {
                return "Quarkus properties";
            }
        });
    }
}
