/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
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
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.NOTIFICATION_GROUP;

public class QuarkusPreLoadingActivity extends PreloadingActivity {
    @Override
    public void preload(@NotNull ProgressIndicator indicator) {
        if (PluginHelper.isLSPPluginInstalledAndNotUsed()) {
            Notification notification = new Notification(NOTIFICATION_GROUP, AllIcons.General.Warning,
                    NOTIFICATION_GROUP, null,
                    "LSP Support plugin in enabled but not used by any plugin, it may causes issues and Quarkus Tools does not depend on it anymore so you better disable or remove it.",
                    NotificationType.WARNING, null);
            Notifications.Bus.notify(notification);
        }
    }
}
