package com.redhat.devtools.intellij.quarkus.tool;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_DEPLOYMENT_PROPERTY_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_EXTENSION_PROPERTIES;

public interface ToolDelegate  {
    static String getDeploymentJarId(File file) {
        String result = null;
        if (file.isDirectory()) {
            File quarkusFile = new File(file, QUARKUS_EXTENSION_PROPERTIES);
            if (quarkusFile.exists()) {
                try (Reader r = new FileReader(quarkusFile)) {
                    result = getQuarkusExtension(r);
                } catch (IOException e) {}
            }
        } else {
            try {
                JarFile jarFile = new JarFile(file);
                JarEntry entry = jarFile.getJarEntry(QUARKUS_EXTENSION_PROPERTIES);
                if (entry != null) {
                    try (Reader r = new InputStreamReader(jarFile.getInputStream(entry),"UTF-8")) {
                        result = getQuarkusExtension(r);
                    }
                }
            } catch (IOException e) {}
        }
        return result;
    }

    static String getQuarkusExtension(Reader r) throws IOException {
        Properties p = new Properties();
        p.load(r);
        return p.getProperty(QUARKUS_DEPLOYMENT_PROPERTY_NAME);
    }

    boolean isValid(Module module);
    List<VirtualFile> getDeploymentFiles(Module module);

    static final ExtensionPointName<ToolDelegate> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.toolDelegate");

    public static List<VirtualFile> scanDeploymentFiles(Module module) {
        for(ToolDelegate delegate : EP_NAME.getExtensions()) {
            if (delegate.isValid(module)) {
                return delegate.getDeploymentFiles(module);
            }
        }
        return Collections.emptyList();
    }
}
