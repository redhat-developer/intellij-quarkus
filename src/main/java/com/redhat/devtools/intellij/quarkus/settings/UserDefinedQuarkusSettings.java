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
package com.redhat.devtools.intellij.quarkus.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * User defined Quarkus settings for:
 *
 * <ul>
 *     <li>auto create Quarkus run configuration</li>
 * </ul>
 */
@State(
        name = "QuarkusSettingsState",
        storages = {@Storage("quarkusSettings.xml")}
)
public class UserDefinedQuarkusSettings implements PersistentStateComponent<UserDefinedQuarkusSettings.MyState> {

    private volatile MyState myState = new MyState();

    private final Project project;

    public UserDefinedQuarkusSettings(Project project) {
        this.project = project;
    }

    private final List<Runnable> myChangeHandlers = ContainerUtil.createConcurrentList();

    public static @NotNull UserDefinedQuarkusSettings getInstance(@NotNull Project project) {
        return project.getService(UserDefinedQuarkusSettings.class);
    }

    public void addChangeHandler(Runnable runnable) {
        myChangeHandlers.add(runnable);
    }

    public void removeChangeHandler(Runnable runnable) {
        myChangeHandlers.remove(runnable);
    }

    public void fireStateChanged() {
        for (Runnable handler : myChangeHandlers) {
            handler.run();
        }
    }
    public boolean isCreateQuarkusRunConfigurationOnProjectImport() {
        return myState.myCreateQuarkusRunConfigurationOnProjectImport;
    }

    public void setCreateQuarkusRunConfigurationOnProjectImport(boolean createQuarkusRunConfigurationOnProjectImport) {
        myState.myCreateQuarkusRunConfigurationOnProjectImport = createQuarkusRunConfigurationOnProjectImport;
    }

    @Nullable
    @Override
    public MyState getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull MyState state) {
        myState = state;
        for (Runnable handler : myChangeHandlers) {
            handler.run();
        }
    }

    public static class MyState {

        @Tag("createQuarkusRunConfigurationOnProjectImport")
        public boolean myCreateQuarkusRunConfigurationOnProjectImport = true;

        MyState() {
        }

    }

}
