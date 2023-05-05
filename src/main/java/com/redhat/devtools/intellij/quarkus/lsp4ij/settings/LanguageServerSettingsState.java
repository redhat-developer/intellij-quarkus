/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.quarkus.lsp4ij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings for a given Language server definition
 *
 * <ul>
 *     <li>Debug port</li>
 *     <li>Suspend and wait for a debugger</li>
 * </ul>
 */
@State(
        name = "com.redhat.devtools.intellij.lsp4ij.settings.LanguageServerSettingsState",
        storages = {@Storage("LanguageServersSettings.xml")}
)
public class LanguageServerSettingsState implements PersistentStateComponent<LanguageServerSettingsState> {

    private Map<String, LanguageServerDefinitionSettings> languageServers = new HashMap<>();

    public static LanguageServerSettingsState getInstance() {
        return ServiceManager.getService(LanguageServerSettingsState.class);
    }

    @Nullable
    @Override
    public LanguageServerSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull LanguageServerSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public LanguageServerDefinitionSettings getLanguageServerSettings(String languageSeverId) {
        return languageServers.get(languageSeverId);
    }

    public void setLanguageServerSettings(String languageSeverId, LanguageServerDefinitionSettings settings) {
        languageServers.put(languageSeverId, settings);
    }

    public static class LanguageServerDefinitionSettings {

        private String debugPort;

        private boolean debugSuspend;

        public String getDebugPort() {
            return debugPort;
        }

        public void setDebugPort(String debugPort) {
            this.debugPort = debugPort;
        }

        public boolean isDebugSuspend() {
            return debugSuspend;
        }

        public void setDebugSuspend(boolean debugSuspend) {
            this.debugSuspend = debugSuspend;
        }
    }


}
