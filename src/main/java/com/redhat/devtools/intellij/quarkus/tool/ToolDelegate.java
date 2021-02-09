/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.tool;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
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

    public static ToolDelegate getDelegate(Module module) {
        for(ToolDelegate toolDelegate : getDelegates()) {
            if (toolDelegate.isValid(module)) {
                return toolDelegate;
            }
        }
        return null;
    }

    /**
     * Checks if this delegate is valid for the module.
     *
     * @param module the module to process
     * @return true if delegate is in charge, false otherwise
     */
    boolean isValid(Module module);

    public static final int BINARY = 0;

    public static final int SOURCES = 1;

    /**
     * Return the list of additional deployment JARs for the module. The array should have 2 elements, the first
     * one being for binary JARs, the second one for sources JARs.
     *
     * @param module the module to process
     * @return the list of additional deployment JARs for the module
     * @see #BINARY
     * @see #SOURCES
     */
    List<VirtualFile>[] getDeploymentFiles(Module module);

    /**
     * Returns the displayable string for the delegate.
     *
     * @return the displayable string for the delegate
     */
    String getDisplay();

    /**
     * Use to sort the list in the module builder.
     *
     * @return the display order, lower values are displayed first
     */
    default int getOrder() {
        return 0;
    }

    /**
     * Return the value of the parameter build to be given to code.quarkus.io.
     *
     * @return the build tool parameter value
     */
    default String asParameter() {
        return getDisplay().toUpperCase();
    }

    /**
     * Process tool specific module initialization.
     *
     * @param module the module to process
     */
    void processImport(Module module);

    default VirtualFile getJarFile(String path) {
        return getJarFile(new File(path));
    }

    default VirtualFile getJarFile(File file) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        return virtualFile != null? JarFileSystem.getInstance().getJarRootForLocalFile(virtualFile):null;
    }

    static final ExtensionPointName<ToolDelegate> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.toolDelegate");

    @NotNull
    static List<VirtualFile>[] initDeploymentFiles() {
        List<VirtualFile>[] result = new List[2];
        result[0] = new ArrayList<>();
        result[1] = new ArrayList<>();
        return result;
    }

    public static List<VirtualFile>[] scanDeploymentFiles(Module module) {
        for(ToolDelegate delegate : EP_NAME.getExtensions()) {
            if (delegate.isValid(module)) {
                return delegate.getDeploymentFiles(module);
            }
        }
        return initDeploymentFiles();
    }

    public static ToolDelegate[] getDelegates() {
        ToolDelegate[] delegates = EP_NAME.getExtensions();
        Arrays.sort(delegates, (a,b) -> a.getOrder() - b.getOrder());
        return delegates;
    }
}
