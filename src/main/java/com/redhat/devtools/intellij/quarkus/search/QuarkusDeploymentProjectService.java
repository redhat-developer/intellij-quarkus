/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@State(
    name = "QuarkusDeploymentProjectService",
    storages = @Storage("QuarkusDeploymentProjectService.xml")
)
public final class QuarkusDeploymentProjectService implements PersistentStateComponent<QuarkusDeploymentProjectService.State> {

    public static class State {
        public Map<String, ModuleState> modules = new HashMap<>();
    }

    public static class ModuleState {
        public Integer hash;
        public Integer version;
    }

    private final @NotNull Project project;
    private State state = new State();

    public QuarkusDeploymentProjectService(@NotNull Project project) {
        this.project = project;

        // Listen for module removal to clean up the map
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(ModuleListener.TOPIC, new ModuleListener() {
            @Override
            public void moduleRemoved(@NotNull Project project, @NotNull Module module) {
                state.modules.remove(module.getName());
            }
        });
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    /**
     * Returns the persisted state for the given module.
     * Creates a new state if none exists yet.
     */
    public ModuleState getModuleState(Module module) {
        return state.modules.computeIfAbsent(module.getName(), k -> new ModuleState());
    }

    // Convenience accessors
    public Integer getHash(Module module) {
        ModuleState m = getModuleState(module);
        return m.hash;
    }

    public void setHash(Module module, Integer hash) {
        ModuleState m = getModuleState(module);
        m.hash = hash;
    }

    public Integer getVersion(Module module) {
        ModuleState m = getModuleState(module);
        return m.version;
    }

    public void setVersion(Module module, Integer version) {
        ModuleState m = getModuleState(module);
        m.version = version;
    }
}
