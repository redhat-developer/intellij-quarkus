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
package com.redhat.devtools.intellij.qute.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.Tag;
import com.redhat.devtools.lsp4ij.features.diagnostics.SeverityMapping;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User defined Qute settings
 *
 * <ul>
 *     <li>validation</li>
 * </ul>
 */
@State(
        name = "QuteSettingsState",
        storages = {@Storage("quteSettings.xml")}
)
public class UserDefinedQuteSettings implements PersistentStateComponent<UserDefinedQuteSettings.MyState> {

    private volatile MyState myState = new MyState();

    private final Project project;

    public UserDefinedQuteSettings(Project project) {
        this.project = project;
    }

    private final List<Runnable> myChangeHandlers = ContainerUtil.createConcurrentList();

    public static @NotNull UserDefinedQuteSettings getInstance(@NotNull Project project) {
        return project.getService(UserDefinedQuteSettings.class);
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

    public boolean isValidationEnabled() {
        return myState.myValidationEnabled;
    }

    public void setValidationEnabled(boolean validationEnabled) {
        myState.myValidationEnabled = validationEnabled;
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

    /**
     * Returns the proper settings expected by the Qute language server.
     *
     * @return the proper settings expected by the Qute language server.
     */
    public Map<String, Object> toSettingsForQuteLS() {
        /*
        "settings": {
        "qute": {
            "server": {
                "vmargs": "-Xmx100M -XX:+UseG1GC -XX:+UseStringDeduplication -Xlog:disable"
            },
            "templates": {
                "languageMismatch": "force"
            },
            "trace": {
                "server": "verbose"
            },
            "codeLens": {
                "enabled": true
            },
            "inlayHint": {
                "enabled": true,
                "showSectionParameterType": true,
                "showSectionParameterDefaultValue": true
            },
            "native": {
                "enabled": false
            },
            "validation": {
                "enabled": true,
                "excluded": [],
                "undefinedObject": {
                    "severity": "warning"
                },
                "undefinedNamespace": {
                    "severity": "warning"
                }
            }
        }
        */
        QuteInspectionsInfo inspectionsInfo = QuteInspectionsInfo.getQuteInspectionsInfo(project);
        Map<String, Object> settings = new HashMap<>();

        Map<String, Object> qute = new HashMap<>();
        settings.put("qute", qute);
        qute.put("workspaceFolders", new HashMap<String, Object>());

        // Inlay hint
        qute.put("inlayHint", Collections.singletonMap("enabled", true));

        //Code lens
        qute.put("codelens", Collections.singletonMap("enabled", true));

        //Native mode support
        qute.put("native", Collections.singletonMap("enabled", isNativeModeSupportEnabled()));

        // Validation
        Map<String, Object> validation = new HashMap<>();
        qute.put("validation", validation);

        validation.put("enabled", inspectionsInfo.enabled());
        validation.put("excluded", inspectionsInfo.getExcludedFiles());

        validation.put("undefinedObject", getSeverityNode(inspectionsInfo.undefinedObjectSeverity()));
        validation.put("undefinedNamespace", getSeverityNode(inspectionsInfo.undefinedNamespaceSeverity()));
        return settings;
    }

    private Map<String, String> getSeverityNode(DiagnosticSeverity severity) {
        return Collections.singletonMap("severity", SeverityMapping.toString(severity));
    }

    public boolean isNativeModeSupportEnabled() {
        return myState.myNativeModeSupportEnabled;
    }

    public void setNativeModeSupportEnabled(boolean nativeModeSupportEnabled) {
        myState.myNativeModeSupportEnabled = nativeModeSupportEnabled;
    }

    public static class MyState {

        @Tag("validationEnabled")
        public boolean myValidationEnabled = true;

        @Tag("nativeModeSupportEnabled")
        public boolean myNativeModeSupportEnabled;

        MyState() {
        }

    }

}
