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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ui.InspectionOptionsPanel;
import com.intellij.codeInspection.ui.ListEditForm;
import com.redhat.devtools.intellij.lsp4mp4ij.MicroProfileBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * No-op {@link LocalInspectionTool} used as a basis for mapping properties inspection severities to matching LSP severities.
 * Adds the possibility to define excluded properties.
 */
public abstract class AbstractDelegateInspectionWithExcludedProperties extends LocalInspectionTool {

    public final @NotNull List<String> excludeList = new ArrayList<>();

    public JComponent createOptionsPanel() {
        InspectionOptionsPanel panel = new InspectionOptionsPanel();

        var injectionListTable = new ListEditForm("", MicroProfileBundle.message("microprofile.properties.validation.excluded.properties"), excludeList);

        panel.addGrowing(injectionListTable.getContentPanel());

        return panel;
    }
}
