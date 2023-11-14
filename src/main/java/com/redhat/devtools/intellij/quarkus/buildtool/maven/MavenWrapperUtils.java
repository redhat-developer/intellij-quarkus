// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.redhat.devtools.intellij.quarkus.buildtool.maven;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Port of <a href="https://github.com/JetBrains/intellij-community/blob/fa32fd18b2ea30ef9994f9737304df918ba12055/plugins/maven/src/main/java/org/jetbrains/idea/maven/server/MavenWrapperSupport.kt#L177-L187">MavenWrapperSupport.getWrapperDistributionUrl</a> to Java
 */
public class MavenWrapperUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenWrapperUtils.class);
    private static final String DISTRIBUTION_URL_PROPERTY = "distributionUrl";
    private MavenWrapperUtils(){}

    public static String getWrapperDistributionUrl(VirtualFile baseDir) {
        VirtualFile wrapperPropertiesFile = getWrapperProperties(baseDir);
        if (wrapperPropertiesFile == null) {
            return null;
        }

        try (ByteArrayInputStream stream = new ByteArrayInputStream(wrapperPropertiesFile.contentsToByteArray(true))) {
            Properties properties = new Properties();
            properties.load(stream);
            return properties.getProperty(DISTRIBUTION_URL_PROPERTY);
        } catch (IOException e) {
            LOGGER.warn("Failed to read Maven Wrapper from "+baseDir, e);
        }
        return null;
    }

    public static @Nullable VirtualFile getWrapperProperties(VirtualFile baseDir) {
        if (baseDir != null) {
            VirtualFile mvnDir = baseDir.findChild(".mvn");
            if (mvnDir != null) {
                VirtualFile wrapperDir = mvnDir.findChild("wrapper");
                if (wrapperDir != null) {
                    return wrapperDir.findChild("maven-wrapper.properties");
                }
            }
        }
        return null;
    }
}