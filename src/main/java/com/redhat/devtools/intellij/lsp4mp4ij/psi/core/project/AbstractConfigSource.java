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

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for config file.
 *
 * @param <T> the config model (ex: Properties for *.properties file)
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/AbstractConfigSource.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/AbstractConfigSource.java</a>
 */
public abstract class AbstractConfigSource<T> implements IConfigSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfigSource.class);

    private static final int DEFAULT_ORDINAL = 100;

    private final String configFileName;

    private final String profile;

    private final int ordinal;

    private final Module javaProject;
    private VirtualFile outputConfigFile;
    private VirtualFile sourceConfigFile;
    private long lastModified = -1L;
    private T config;

    private Map<String, List<MicroProfileConfigPropertyInformation>> propertyInformations;

    public AbstractConfigSource(String configFileName, int ordinal, Module javaProject) {
        this(configFileName, null, ordinal, javaProject);
    }

    public AbstractConfigSource(String configFileName, Module javaProject) {
        this(configFileName, null, javaProject);
    }

    public AbstractConfigSource(String configFileName, String profile, Module javaProject) {
        this(configFileName, profile, DEFAULT_ORDINAL, javaProject);
    }

    public AbstractConfigSource(String configFileName, String profile, int ordinal, Module javaProject) {
        this.configFileName = configFileName;
        this.profile = profile;
        this.ordinal = ordinal;
        this.javaProject = javaProject;
        // load config file to udpate some fields like lastModified, config instance
        // which must be updated when the config source is created. It's important that
        // those fields are initialized here (and not in lazy mode) to prevent from
        // multi
        // thread
        // context.
        init();
    }

    private void init() {
        T config = getConfig();
        if (config != null && propertyInformations == null) {
            propertyInformations = loadPropertyInformations();
        }
    }

    /**
     * Returns the target/classes/$configFile and null otherwise.
     *
     * <p>
     * Using this file instead of using src/main/resources/$configFile gives the
     * capability to get the filtered value.
     * </p>
     *
     * @return the target/classes/$configFile and null otherwise.
     */
    private VirtualFile getOutputConfigFile() {
        if (outputConfigFile != null && outputConfigFile.exists()) {
            return outputConfigFile;
        }
        sourceConfigFile = null;
        outputConfigFile = null;
        if (javaProject.isLoaded()) {
            VirtualFile[] sourceRoots = ModuleRootManager.getInstance(javaProject).getSourceRoots(false);
            for (VirtualFile sourceRoot : sourceRoots) {
                VirtualFile file = sourceRoot.findFileByRelativePath(configFileName);
                if (file != null && file.exists()) {
                    sourceConfigFile = file;
                    outputConfigFile = file;
                }
            }
            VirtualFile output = CompilerPaths.getModuleOutputDirectory(javaProject, false);
            if (output != null) {
                output = output.findFileByRelativePath(configFileName);
                if (output != null) {
                    if (sourceConfigFile == null || output.getModificationStamp() >= sourceConfigFile.getModificationStamp()) {
                        outputConfigFile = output;
                    }
                }
            }
            return outputConfigFile;
        }
        return null;
    }

    @Override
    public String getConfigFileName() {
        return configFileName;
    }

    @Override
    public String getProfile() {
        return profile;
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public String getSourceConfigFileURI() {
        getOutputConfigFile();
        if (sourceConfigFile != null) {
            String uri = sourceConfigFile.getUrl();
            return fixURI(uri);
        }
        return null;
    }

    @Override
    public boolean isSourceConfigFile(VirtualFile file) {
        return file.equals(sourceConfigFile);
    }

    private static String fixURI(String uri) {
        return VfsUtil.toUri(uri).toString();
    }

    /**
     * Returns the loaded config and null otherwise.
     *
     * @return the loaded config and null otherwise
     */
    protected final T getConfig() {
        VirtualFile configFile = getOutputConfigFile();
        if (configFile == null) {
            reset();
            return null;
        }
        try {
            long currentLastModified = configFile.getModificationStamp();
            if (currentLastModified > lastModified) {
                reset();
                try (InputStream input = configFile.getInputStream()) {
                    config = loadConfig(input);
                    lastModified = configFile.getModificationStamp();
                } catch (Exception e) {
                    reset();
                    LOGGER.warn("Error while loading properties from '" + configFile + "'.", e);
                }
            }
        } catch (RuntimeException e1) {
            LOGGER.warn("Error while getting last modified time for '" + configFile + "'.", e1);
        }
        return config;
    }

    @Override
    public void reload(PsiFile file) {
        reset();
        String content = file.getText();
        try (InputStream input = IOUtils.toInputStream(content, Charset.defaultCharset())) {
            config = loadConfig(input);
            lastModified = System.currentTimeMillis();
        } catch (Exception e) {
            reset();
            LOGGER.warn("Error while loading properties from '" + sourceConfigFile + "'.", e);
        }
    }

    @Override
    public Integer getPropertyAsInt(String key) {
        String property = getProperty(key);
        if (property != null && !property.trim().isEmpty()) {
            try {
                return Integer.parseInt(property.trim());
            } catch (NumberFormatException e) {
                LOGGER.warn("Error while converting '" + property.trim() + "' as Integer for key '" + key + "'", e);
                return null;
            }
        }
        return null;
    }

    private void reset() {
        config = null;
        propertyInformations = null;
    }

    @Override
    public List<MicroProfileConfigPropertyInformation> getPropertyInformations(String propertyKey) {
        init();
        return propertyInformations != null ? propertyInformations.get(propertyKey) : null;
    }

    /**
     * Load the config model from the given input stream <code>input</code>.
     *
     * @param input the input stream
     * @return he config model from the given input stream <code>input</code>.
     * @throws IOException
     */
    protected abstract T loadConfig(InputStream input) throws IOException;

    /**
     * Load the property informations.
     *
     * @return the property information.
     */
    protected abstract Map<String /* property key without profile */, List<MicroProfileConfigPropertyInformation>> loadPropertyInformations();
}
