/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.template;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.qute.psi.internal.template.datamodel.DataModelProviderRegistry;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.qute.commons.QuteProjectScope;
import com.redhat.qute.commons.binary.BinaryTemplate;
import com.redhat.qute.commons.binary.BinaryTemplateInfo;
import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelProject;
import com.redhat.qute.commons.datamodel.DataModelTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Support for Quarkus integration for Qute which collect parameters information
 * (a name and a Java type) for Qute template. This collect uses several
 * strategies :
 *
 * <ul>
 * <li>@CheckedTemplate support: collect parameters for Qute Template by
 * searching @CheckedTemplate annotation.</li>
 * <li>Template field support: collect parameters for Qute Template by searching
 * Template instance declared as field in Java class.</li>
 * <li>Template extension support: see
 * https://quarkus.io/guides/qute-reference#template_extension_methods</li>
 * </ul>
 *
 * @author Angelo ZERR
 * @see <a href="https://quarkus.io/guides/qute-reference#quarkus_integration">https://quarkus.io/guides/qute-reference#quarkus_integration</a>
 * @see <a href="https://quarkus.io/guides/qute-reference#typesafe_templates">https://quarkus.io/guides/qute-reference#typesafe_templates</a>
 * @see <a href="https://quarkus.io/guides/qute-reference#template_extension_methods">https://quarkus.io/guides/qute-reference#template_extension_methods</a>
 */
public class QuarkusIntegrationForQute {

    private static final String TEMPLATES_ENTRY = "templates";
    private static final String APPLICATION_PROPERTIES_ENTRY = "application.properties";
    private static final Logger LOGGER = Logger.getLogger(QuarkusIntegrationForQute.class.getName());

    public static DataModelProject<DataModelTemplate<DataModelParameter>> getDataModelProject(Module javaProject,
                                                                                              IPsiUtils utils,
                                                                                              ProgressIndicator monitor) {
        return DataModelProviderRegistry.getInstance().getDataModelProject(javaProject,
                QuteProjectScope.SOURCES_AND_DEPENDENCIES, utils, monitor);
    }

    /**
     * Collect binary templates from all JAR dependencies of the given module.
     * Templates are read from the {@code templates/} entry and its sub-folders
     * inside each JAR.
     *
     * @param javaProject the IntelliJ module.
     * @param monitor     the progress indicator.
     * @return the list of binary template infos found.
     */
    public static List<BinaryTemplateInfo> getBinaryTemplates(Module javaProject, ProgressIndicator monitor) {
        List<BinaryTemplateInfo> binaryTemplates = new ArrayList<>();

        VirtualFile[] roots = ModuleRootManager.getInstance(javaProject)
                .orderEntries()
                .withoutModuleSourceEntries()  // skip source roots, libraries only
                .classes()
                .getRoots();

        for (VirtualFile root : roots) {
            BinaryTemplateInfo info = collectBinaryTemplates(root);
            if (info != null) {
                binaryTemplates.add(info);
            }
        }
        return binaryTemplates;
    }

    /**
     * Collects binary templates for a single classpath root (JAR).
     *
     * @param root the classpath root VirtualFile.
     * @return a {@link BinaryTemplateInfo} if templates were found, {@code null} otherwise.
     */
    private static BinaryTemplateInfo collectBinaryTemplates(VirtualFile root) {
        List<BinaryTemplate> templates = new ArrayList<>();

        // Look for the 'templates' directory at the root of the JAR
        VirtualFile templatesDir = root.findFileByRelativePath(TEMPLATES_ENTRY);
        if (templatesDir != null && templatesDir.exists() && templatesDir.isDirectory()) {
            collectTemplatesRecursively(templatesDir, "", templates);
        }

        if (templates.isEmpty()) {
            return null;
        }

        BinaryTemplateInfo info = new BinaryTemplateInfo();
        info.setBinaryName(root.getName());
        info.setTemplates(templates);

        // Look for application.properties at the root of the JAR
        VirtualFile appProps = root.findFileByRelativePath(APPLICATION_PROPERTIES_ENTRY);
        if (appProps != null && appProps.exists() && !appProps.isDirectory()) {
            info.setProperties(parseProperties(appProps));
        }

        return info;
    }

    /**
     * Recursively collects template files from a directory VirtualFile.
     *
     * @param dir         the current directory to scan.
     * @param currentPath the relative path from {@code templates/} (empty for root).
     * @param templates   the list to fill.
     */
    private static void collectTemplatesRecursively(VirtualFile dir, String currentPath,
                                                    List<BinaryTemplate> templates) {
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                String childPath = currentPath.isEmpty() ? child.getName() : currentPath + "/" + child.getName();
                collectTemplatesRecursively(child, childPath, templates);
            } else {
                try {
                    String fileName = child.getName();
                    String path = currentPath.isEmpty() ? fileName : currentPath + "/" + fileName;
                    String uri = toUri(child);
                    String content = convertStreamToString(child.getInputStream());

                    BinaryTemplate template = new BinaryTemplate();
                    template.setPath(path);
                    template.setUri(uri);
                    template.setContent(content);
                    templates.add(template);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
            }
        }
    }

    /**
     * Parses an {@code application.properties} VirtualFile into a key/value Map.
     *
     * @param propertiesFile the VirtualFile for application.properties.
     * @return a Map of property key/value pairs, or {@code null} on error.
     */
    private static Map<String, String> parseProperties(VirtualFile propertiesFile) {
        Properties props = new Properties();
        try (InputStream is = propertiesFile.getInputStream()) {
            props.load(is);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while loading application.properties from JAR entry", e);
            return null;
        }
        Map<String, String> map = new java.util.LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            map.put(key, props.getProperty(key));
        }
        return map;
    }

    /**
     * Convert the given {@link InputStream} into a String. The source InputStream
     * will then be closed.
     *
     * @param is the input stream
     * @return the given input stream in a String.
     */
    private static String convertStreamToString(InputStream is) {
        try (Scanner s = new Scanner(is)) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    // see
    // https://github.com/microsoft/vscode-java-dependency/blob/27c306b770c23b1eba1f9a7c3e70d2793baced68/jdtls.ext/com.microsoft.jdtls.ext.core/src/com/microsoft/jdtls/ext/core/ExtUtils.java#L39

    private static String toUri(VirtualFile jarEntryFile) {
        return LSPIJUtils.toUriAsString(jarEntryFile);
    }
}
