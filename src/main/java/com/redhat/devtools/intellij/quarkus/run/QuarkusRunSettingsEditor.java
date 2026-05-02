/*******************************************************************************
 * Copyright (c) 2022-2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.run;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ui.*;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.util.Predicates;
import com.intellij.ui.RawCommandLineEditor;
import com.redhat.devtools.intellij.quarkus.run.fragments.QuarkusModuleFragment;
import com.redhat.devtools.intellij.quarkus.run.fragments.QuarkusProfileFragment;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.execution.ui.CommandLinePanel.setMinimumWidth;

/**
 * Settings editor for Quarkus run configurations using fragmented approach.
 */
public class QuarkusRunSettingsEditor extends RunConfigurationFragmentedEditor<QuarkusRunConfiguration> {

    public QuarkusRunSettingsEditor(@NotNull QuarkusRunConfiguration runConfiguration) {
        super(runConfiguration);
    }

    @Override
    protected @NotNull List<SettingsEditorFragment<QuarkusRunConfiguration, ?>> createRunFragments() {
        List<SettingsEditorFragment<QuarkusRunConfiguration, ?>> fragments = new ArrayList<>();

        // Before Run tasks
        BeforeRunComponent beforeRunComponent = new BeforeRunComponent(this);
        fragments.add(BeforeRunFragment.createBeforeRun(beforeRunComponent, CompileStepBeforeRun.ID));
        fragments.addAll(BeforeRunFragment.createGroup());

        // Module fragment
        QuarkusModuleFragment moduleFragment = new QuarkusModuleFragment(getProject());
        fragments.add(moduleFragment);

        // Quarkus specific fragments (right after module)
        fragments.add(new QuarkusProfileFragment());

        // Common parameter fragments
        CommonParameterFragments<QuarkusRunConfiguration> commonParameterFragments =
            new CommonParameterFragments<>(getProject(), () -> moduleFragment.getSelectedModule());

        // Program arguments right after profile
        SettingsEditorFragment<QuarkusRunConfiguration, ?> programArgsFragment = commonParameterFragments.programArguments();
        programArgsFragment.setHint("Arguments passed to the Quarkus application (via -Dquarkus.args)");
        fragments.add(programArgsFragment);

        // Add other environment fragments
        fragments.add(CommonParameterFragments.createEnvParameters());

        return fragments;
    }
}
