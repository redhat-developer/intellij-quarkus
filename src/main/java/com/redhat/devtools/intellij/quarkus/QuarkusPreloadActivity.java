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

import com.intellij.icons.AllIcons;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Version;
import org.jetbrains.annotations.NotNull;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.DISPLAY_CHECK_NOTIFACTION_PROPERTY_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.NOTIFICATION_GROUP;

public class QuarkusPreloadActivity extends PreloadingActivity {
    @Override
    public void preload(@NotNull ProgressIndicator indicator) {
        checkLSPPlugin();
    }

    private void checkLSPPlugin() {
        IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(PluginId.getId(QuarkusConstants.LSP_PLUGIN_ID));
        if (shouldDisplayNotification()) {
            if (pluginDescriptor == null) {
                displayNotification("Language Server protocol support not installed, code assist on application.properties will not be provided");
            } else if (Version.parseVersion(pluginDescriptor.getVersion()).lessThan(1, 5, 5)) {
                displayNotification("Language Server protocol support installed, code assist on application.properties is available but some issues may happen");
            }
        }
    }

    private void displayNotification(String content) {
        Notification notification = new Notification(NOTIFICATION_GROUP, AllIcons.General.Warning, "Quarkus", null, content, NotificationType.WARNING, null);
        Notifications.Bus.notify(notification);
        resetNotification();
    }

    private boolean shouldDisplayNotification() {
        return PropertiesComponent.getInstance().getBoolean(DISPLAY_CHECK_NOTIFACTION_PROPERTY_NAME, true);
    }

    private void resetNotification() {
        PropertiesComponent.getInstance().setValue(DISPLAY_CHECK_NOTIFACTION_PROPERTY_NAME, false, true);
    }
}
