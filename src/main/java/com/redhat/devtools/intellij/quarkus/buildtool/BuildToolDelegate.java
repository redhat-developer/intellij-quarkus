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
package com.redhat.devtools.intellij.quarkus.buildtool;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenId;

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

public interface BuildToolDelegate {

    static boolean shouldResolveArtifactTransitively(MavenId deploymentId) {
        // The kubernetes support is only available if quarkus-kubernetes artifact is
        // declared in the pom.xml
        // When quarkus-kubernetes is declared, this JAR declares the deployment JAR
        // quarkus-kubernetes-deployment
        // This quarkus-kubernetes-deployment artifact has some dependencies to
        // io.dekorate

        // In other words, to add
        // io.dekorate.kubernetes.annotation.KubernetesApplication class in the search
        // classpath,
        // the dependencies of quarkus-kubernetes-deployment artifact must be downloaded
        return "quarkus-kubernetes-deployment".equals(deploymentId.getArtifactId())
                || "quarkus-openshift-deployment".equals(deploymentId.getArtifactId())
                || "quarkus-smallrye-openapi-deployment".equals(deploymentId.getArtifactId());
    }

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

    static boolean hasExtensionProperties(File file) {
        return getDeploymentJarId(file) != null;
    }

    static String getQuarkusExtension(Reader r) throws IOException {
        Properties p = new Properties();
        p.load(r);
        return p.getProperty(QUARKUS_DEPLOYMENT_PROPERTY_NAME);
    }

    public static BuildToolDelegate getDelegate(Module module) {
        for(BuildToolDelegate toolDelegate : getDelegates()) {
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
    List<VirtualFile>[] getDeploymentFiles(Module module, ProgressIndicator progressIndicator);

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

    static final ExtensionPointName<BuildToolDelegate> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.toolDelegate");

    @NotNull
    static List<VirtualFile>[] initDeploymentFiles() {
        List<VirtualFile>[] result = new List[2];
        result[0] = new ArrayList<>();
        result[1] = new ArrayList<>();
        return result;
    }

    public static BuildToolDelegate[] getDelegates() {
        BuildToolDelegate[] delegates = EP_NAME.getExtensions();
        Arrays.sort(delegates, (a,b) -> a.getOrder() - b.getOrder());
        return delegates;
    }

    RunnerAndConfigurationSettings getConfigurationDelegate(Module module, QuarkusRunConfiguration configuration);

    /**
     * Add project import listener.
     *
     * @param project the project.
     * @param connection the project connection used to subscribe maven, gradle listener which tracks project import.
     * @param listener the project import listener.
     */
    void addProjectImportListener(@NotNull Project project,  @NotNull MessageBusConnection connection, @NotNull ProjectImportListener listener);
}
