/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.project.ConfigSourcePropertiesProvider;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.eclipse.lsp4mp.commons.utils.ConfigSourcePropertiesProviderUtils;
import org.eclipse.lsp4mp.commons.utils.IConfigSourcePropertiesProvider;
import org.eclipse.lsp4mp.commons.utils.PropertyValueExpander;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JDT MicroProfile project.
 *
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/project/JDTMicroProfileProject.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/project/JDTMicroProfileProject.java</a>
 */
public class PsiMicroProfileProject {

    private final Module javaProject;

    private List<IConfigSource> configSources;

    private transient IConfigSourcePropertiesProvider aggregatedPropertiesProvider = null;
    private transient PropertyValueExpander propertyValueExpander = null;

    public PsiMicroProfileProject(Module javaProject) {
        this.javaProject = javaProject;
    }

    /**
     * Returns the value of this property or <code>defaultValue</code> if it is not
     * defined in this project.
     * <p>
     * Expands property expressions when there are no cyclical references between
     * property values.
     * </p>
     *
     * @param propertyKey  the property to get with the profile included, in the
     *                     format used by microprofile-config.properties
     * @param defaultValue the value to return if the value for the property is not
     *                     defined in this project
     * @return the value of this property or <code>defaultValue</code> if it is not
     * defined in this project
     */
    public String getProperty(String propertyKey, String defaultValue) {

        if (aggregatedPropertiesProvider == null) {
            aggregatedPropertiesProvider = getAggregatedPropertiesProvider();
        }

        String unresolved = aggregatedPropertiesProvider.getValue(propertyKey);
        if (unresolved == null) {
            return defaultValue;
        } else if (unresolved.contains("${")) {
            if (propertyValueExpander == null) {
                propertyValueExpander = new PropertyValueExpander(aggregatedPropertiesProvider);
            }
            String expandedValue = propertyValueExpander.getValue(propertyKey);
            if (expandedValue == null) {
                return defaultValue;
            }
            return expandedValue;
        } else {
            return unresolved;
        }
    }

    /**
     * Returns the value of this property or null if it is not defined in this
     * project.
     * <p>
     * Expands property expressions when there are no cyclical references between
     * property values.
     * </p>
     *
     * @param propertyKey the property to get with the profile included, in the
     *                    format used by microprofile-config.properties
     * @return the value of this property or null if it is not defined in this
     * project
     */
    public String getProperty(String propertyKey) {
        return getProperty(propertyKey, null);
    }

    /**
     * Returns the value of this property as an int, or <code>defaultValue</code>
     * when there is no value or the value cannot be parsed as a String.
     * <p>
     * Expands property expressions when there are no cyclical references between
     * property values.
     * <p/>
     *
     * @param key          the property to get with the profile included, in the
     *                     format used by microprofile-config.properties
     * @param defaultValue the value to return if the value for the property is not
     *                     defined in this project
     * @return the value of this property as an int, or <code>defaultValue</code>
     * when there is no value or the value cannot be parsed as a String
     */
    public Integer getPropertyAsInteger(String key, Integer defaultValue) {
        String value = getProperty(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            int intValue = Integer.parseInt(value);
            return intValue;
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    /**
     * Returns a list of all values for properties and different profiles that are
     * defined in this project.
     *
     * <p>
     * This list contains information for the property (ex : greeting.message) and
     * profile property (ex : %dev.greeting.message).
     * </p>
     *
     * <p>
     * When several properties file (ex : microprofile-config.properties,
     * application.properties, etc) define the same property, it's the file which
     * have the bigger ordinal (see {@link IConfigSource#getOrdinal()} which is
     * returned.
     * </p>
     *
     * <p>
     * Expands property expressions for each of the values of the key when there are
     * no cyclical references between property values.
     * </p>
     *
     * @param propertyKey the name of the property to collect the values for
     * @return a list of all values for properties and different profiles that are
     * defined in this project.
     */
    public List<MicroProfileConfigPropertyInformation> getPropertyInformations(String propertyKey) {
        // Use a map to override property values
        // eg. if application.yaml defines a value for a property it should override the
        // value defined in application.properties
        Map<String, MicroProfileConfigPropertyInformation> propertyToInfoMap = new HashMap<>();
        // Go backwards so that application.properties replaces
        // microprofile-config.properties, etc.
        List<IConfigSource> configSources = getConfigSources();
        for (int i = configSources.size() - 1; i >= 0; i--) {
            IConfigSource configSource = configSources.get(i);
            List<MicroProfileConfigPropertyInformation> propertyInformations = configSource
                    .getPropertyInformations(propertyKey);
            if (propertyInformations != null) {
                for (MicroProfileConfigPropertyInformation propertyInformation : propertyInformations) {
                    propertyToInfoMap.put(propertyInformation.getPropertyNameWithProfile(), propertyInformation);
                }
            }
        }
        return propertyToInfoMap.values().stream() //
                .sorted((a, b) -> {
                    return a.getPropertyNameWithProfile().compareTo(b.getPropertyNameWithProfile());
                }) //
                .map(info -> {
                    String resolved = this.getProperty(info.getPropertyNameWithProfile());
                    return new MicroProfileConfigPropertyInformation(info.getPropertyNameWithProfile(), resolved,
                            info.getSourceConfigFileURI(), info.getConfigFileName());
                }).collect(Collectors.toList());
    }

    public List<IConfigSource> getConfigSources() {
        if (configSources == null) {
            configSources = loadConfigSources(javaProject);
        }
        return configSources;
    }

    /**
     * Evict the config sources cache and related cached information as soon as one
     * of properties, yaml file is saved.
     */
    public void evictConfigSourcesCache(VirtualFile file) {
        final IConfigSource existingConfigSource = findConfigSource(file);
        if (existingConfigSource != null) {
            // The config source file exists, update / delete it from the cache
            boolean updated = ReadAction.compute(() -> {
                PsiFile psiFile = LSPIJUtils.getPsiFile(file, javaProject.getProject());
                if (psiFile != null) {
                    // The config source file has been updated, reload it
                    existingConfigSource.reload(psiFile);
                    return true;
                }
                // The config source file has been deleted, remove it
                return false;
            });
            if (!updated) {
                // Remove from config sources cache, the config source file which has been deleted
                configSources.remove(existingConfigSource);
            }
        } else {
            // The config source file doesn't exist, evict the full cache
            configSources = null;
        }
        propertyValueExpander = null;
        aggregatedPropertiesProvider = null;
    }

    private IConfigSource findConfigSource(VirtualFile file) {
        List<IConfigSource> configSources = getConfigSources();
        for (IConfigSource configSource : configSources) {
            if (configSource.isSourceConfigFile(file)) {
                return configSource;
            }
        }
        return null;
    }

    /**
     * Load config sources from the given project and sort it by using
     * {@link IConfigSource#getOrdinal()}
     *
     * @param javaProject the Java project
     * @return the loaded config sources.
     */
    private synchronized List<IConfigSource> loadConfigSources(Module javaProject) {
        if (configSources != null) {
            // Case when there are several Threads which load config sources, the second
            // Thread should not reload the config sources again.
            return configSources;
        }
        List<IConfigSource> configSources = new ArrayList<>();
        VirtualFile outputFile = CompilerPaths.getModuleOutputDirectory(javaProject, false);
        for (IConfigSourceProvider provider : IConfigSourceProvider.EP_NAME.getExtensions()) {
            configSources.addAll(provider.getConfigSources(javaProject, outputFile));
        }
        Collections.sort(configSources, (a, b) -> b.getOrdinal() - a.getOrdinal());
        return configSources;
    }


    /**
     * Returns true if the given property has a value declared for any profile, and
     * false otherwise.
     *
     * @param property the property to check if there is a value for
     * @return true if the given property has a value declared for any profile, and
     * false otherwise
     */
    public boolean hasProperty(String property) {
        List<IConfigSource> configSources = getConfigSources();
        for (IConfigSource configSource : configSources) {
            if (configSource.getPropertyInformations(property) != null) {
                return true;
            }
        }
        return false;
    }

    private IConfigSourcePropertiesProvider getAggregatedPropertiesProvider() {
        List<IConfigSource> configSources = getConfigSources();
        if (configSources.size() == 0) {
            // Return an empty IConfigSourcePropertiesProvider
            return new IConfigSourcePropertiesProvider() {

                @Override
                public Set<String> keys() {
                    return Collections.emptySet();
                }

                @Override
                public boolean hasKey(String key) {
                    return false;
                }

                @Override
                public String getValue(String key) {
                    return null;
                }

            };
        }
        IConfigSourcePropertiesProvider provider = new ConfigSourcePropertiesProvider(
                configSources.get(configSources.size() - 1));
        for (int i = configSources.size() - 2; i >= 0; i--) {
            provider = ConfigSourcePropertiesProviderUtils
                    .layer(new ConfigSourcePropertiesProvider(configSources.get(i)), provider);
        }
        return provider;
    }

}