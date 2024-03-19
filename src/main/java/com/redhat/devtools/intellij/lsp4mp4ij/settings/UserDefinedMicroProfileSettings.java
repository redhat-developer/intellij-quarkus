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
package com.redhat.devtools.intellij.lsp4mp4ij.settings;

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
 * User defined MicroProfile settings for:
 *
 * <ul>
 *     <li>validation</li>
 *     <li>properties files managed with the MicroProfile language server</li>
 *     <li>Java files managed with the MicroProfile language server</li>
 * </ul>
 */
@State(
        name = "MicroProfileSettingsState",
        storages = {@Storage("microProfileSettings.xml")}
)
public class UserDefinedMicroProfileSettings implements PersistentStateComponent<UserDefinedMicroProfileSettings.MyState> {

    private volatile MyState myState = new MyState();

    private final Project project;

    public UserDefinedMicroProfileSettings(Project project) {
        this.project = project;
    }

    private final List<Runnable> myChangeHandlers = ContainerUtil.createConcurrentList();

    public static @NotNull UserDefinedMicroProfileSettings getInstance(@NotNull Project project) {
        return project.getService(UserDefinedMicroProfileSettings.class);
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

    // ---------- Properties

    public boolean isInlayHintEnabled() {
        return myState.myInlayHintEnabled;
    }

    public void setInlayHintEnabled(boolean inlayHintEnabled) {
        myState.myInlayHintEnabled = inlayHintEnabled;
    }

    // ---------- Java

    public boolean isUrlCodeLensEnabled() {
        return myState.myUrlCodeLensEnabled;
    }

    public void setUrlCodeLensEnabled(boolean urlCodeLensEnabled) {
        myState.myUrlCodeLensEnabled = urlCodeLensEnabled;
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
     * Returns the proper settings expected by the MicroProfile language server.
     *
     * @return the proper settings expected by the MicroProfile language server.
     */
    public Map<String, Object> toSettingsForMicroProfileLS() {
        MicroProfileInspectionsInfo inspectionsInfo = MicroProfileInspectionsInfo.getMicroProfileInspectionInfo(project);
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> microprofile = new HashMap<>();
        settings.put("microprofile", microprofile);
        Map<String, Object> tools = new HashMap<>();
        microprofile.put("tools", tools);

        // Properties settings
        // Inlay hint
        Map<String, Object> inlayHint = new HashMap<>();
        inlayHint.put("enabled", isInlayHintEnabled());
        tools.put("inlayHint", inlayHint);

        // Java settings
        // URL code lens
        Map<String, Object> codeLens = new HashMap<>();
        codeLens.put("urlCodeLensEnabled", isUrlCodeLensEnabled());
        tools.put("codeLens", codeLens);

        Map<String, Object> validation = new HashMap<>();
        tools.put("validation", validation);
        validation.put("enabled", inspectionsInfo.enabled());
        validation.put("syntax", getSeverityNode(inspectionsInfo.syntaxSeverity()));
        validation.put("duplicate", getSeverityNode(inspectionsInfo.duplicateSeverity()));
        validation.put("value", getSeverityNode(inspectionsInfo.valueSeverity()));
        validation.put("required", getSeverityNode(inspectionsInfo.requiredSeverity()));
        validation.put("expression", getSeverityNode(inspectionsInfo.expressionSeverity()));

        validation.put("unknown", getSeverityAndExclusions(inspectionsInfo.unknownSeverity(), inspectionsInfo.getExcludedUnknownProperties()));
        validation.put("unassigned", getSeverityAndExclusions(inspectionsInfo.unassignedSeverity(), inspectionsInfo.getExcludedUnassignedProperties()));

        return settings;
    }

    private Map<String, Object> getSeverityAndExclusions(DiagnosticSeverity severity, List<String> exclusions) {
        Map<String, Object> inspection = new HashMap<>();
        inspection.put("severity", SeverityMapping.toString(severity));
        if (exclusions != null && !exclusions.isEmpty()) {
            inspection.put("excluded", exclusions);
        }
        return inspection;
    }

    private Map<String, String> getSeverityNode(DiagnosticSeverity severity) {
        return Collections.singletonMap("severity", SeverityMapping.toString(severity));
    }

    public static class MyState {

        @Tag("validationEnabled")
        public boolean myValidationEnabled = true;

        @Tag("inlayHintEnabled")
        public boolean myInlayHintEnabled = true;

        @Tag("urlCodeLensEnabled")
        public boolean myUrlCodeLensEnabled = true;

        MyState() {
        }

    }

}
