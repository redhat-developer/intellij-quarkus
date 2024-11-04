/*******************************************************************************
 * Copyright (c) 2022 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.template.rootpath;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.util.KeyedLazyInstanceEP;
import com.redhat.devtools.intellij.qute.psi.internal.AbstractQuteExtensionPointRegistry;
import com.redhat.devtools.intellij.qute.psi.template.rootpath.ITemplateRootPathProvider;
import com.redhat.qute.commons.TemplateRootPath;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry to handle instances of {@link ITemplateRootPathProvider}
 *
 * @author Angelo ZERR
 */
public class TemplateRootPathProviderRegistry extends AbstractQuteExtensionPointRegistry<ITemplateRootPathProvider, TemplateRootPathProviderRegistry.TemplateRootPathProviderBean> {

    private static final Logger LOGGER = Logger.getLogger(TemplateRootPathProviderRegistry.class.getName());

    private static final ExtensionPointName<TemplateRootPathProviderBean> TEMPLATE_ROOT_PATH_PROVIDERS_EXTENSION_POINT_ID = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.qute.templateRootPathProvider");

    public static class TemplateRootPathProviderBean extends KeyedLazyInstanceEP<ITemplateRootPathProvider> {
    }


    private static final TemplateRootPathProviderRegistry INSTANCE = new TemplateRootPathProviderRegistry();

    private TemplateRootPathProviderRegistry() {
        super();
    }

    public static TemplateRootPathProviderRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public ExtensionPointName<TemplateRootPathProviderBean> getProviderExtensionId() {
        return TEMPLATE_ROOT_PATH_PROVIDERS_EXTENSION_POINT_ID;
    }

    /**
     * Returns the template root path list for the given java project.
     *
     * @param javaProject the java project.
     * @return the template root path list for the given java project.
     */
    public List<TemplateRootPath> getTemplateRootPaths(Module javaProject) {
        List<TemplateRootPath> rootPaths = new ArrayList<>();
        for (ITemplateRootPathProvider provider : super.getProviders()) {
            if (provider.isApplicable(javaProject)) {
                try {
                    provider.collectTemplateRootPaths(javaProject, rootPaths);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error while collecting template root path with the provider '"
                            + provider.getClass().getName() + "'.", e);
                }
            }
        }
        return rootPaths;
    }
}
