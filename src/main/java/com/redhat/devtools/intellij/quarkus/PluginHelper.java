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
import com.intellij.openapi.util.Version;

public class PluginHelper {
    static IdeaPluginDescriptor getLSPPluginDescriptor() {
        return PluginManager.getPlugin(PluginId.getId(QuarkusConstants.LSP_PLUGIN_ID));
    }

    static boolean isLSPPluginPre154(IdeaPluginDescriptor pluginDescriptor) {
        return Version.parseVersion(pluginDescriptor.getVersion()).lessThan(1, 5, 5);
    }

    public static boolean isLSPPluginPre154() {
        return isLSPPluginPre154(getLSPPluginDescriptor());
    }
}
