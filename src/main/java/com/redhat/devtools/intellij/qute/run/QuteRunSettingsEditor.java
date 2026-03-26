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
package com.redhat.devtools.intellij.qute.run;

import com.intellij.openapi.project.Project;
import com.redhat.devtools.lsp4ij.dap.configurations.attach.DAPAttachSettingsEditor;
import org.jetbrains.annotations.NotNull;

/**
 * Qure run settings editor.
 */
public class QuteRunSettingsEditor extends DAPAttachSettingsEditor<QuteRunConfiguration> {

    public QuteRunSettingsEditor(@NotNull Project project) {
        super(project);
    }
}
