/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name="Quarkus Tools")
public class QuarkusModuleComponent implements ModuleComponent, PersistentStateComponent<QuarkusModuleComponent.State> {
    private final Module module;
    private State state;

    public static final class State {
        private Integer hash;

        public State() {}

        public Integer getHash() {
            return hash;
        }

        public void setHash(Integer hash) {
            this.hash = hash;
        }
    }

    public QuarkusModuleComponent(Module module) {
        this.module = module;
    }

    @Override
    public void moduleAdded() {
        QuarkusModuleUtil.ensureQuarkusLibrary(module);
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

    public Integer getHash() {
        return state != null ? state.getHash():null;
    }

    public void setHash(Integer hash) {
        if (state == null) {
            state = new State();
        }
        state.setHash(hash);
    }
}
