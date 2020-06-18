/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;

import java.util.stream.Stream;

public class PluginHelper {
    static IdeaPluginDescriptor getLSPPluginDescriptor() {
        return PluginManager.getPlugin(PluginId.getId(QuarkusConstants.LSP_PLUGIN_ID));
    }

    public static boolean isLSPPluginInstalledAndNotUsed() {
        int users = 0;
        IdeaPluginDescriptor lspDescriptor = getLSPPluginDescriptor();
        if (lspDescriptor != null && lspDescriptor.isEnabled()) {
            for(IdeaPluginDescriptor descriptor : PluginManager.getPlugins()) {
                if (descriptor.isEnabled()) {
                    users += Stream.of(descriptor.getDependentPluginIds()).filter(id -> id.equals(PluginId.getId(QuarkusConstants.LSP_PLUGIN_ID))).count();
                    users += Stream.of(descriptor.getOptionalDependentPluginIds()).filter(id -> id.equals(PluginId.getId(QuarkusConstants.LSP_PLUGIN_ID))).count();
                }
            }
        } else {
            users = -1;
        }
        return users == 0;
    }
}
