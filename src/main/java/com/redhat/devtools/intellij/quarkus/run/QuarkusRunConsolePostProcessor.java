/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.run;

import com.intellij.execution.actions.ConsoleActionsPostProcessor;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.NotNull;

public class QuarkusRunConsolePostProcessor extends ConsoleActionsPostProcessor {
    @Override
    public AnAction[] postProcess(@NotNull ConsoleView console, AnAction[] actions) {
        AnAction[] newActions = new AnAction[actions.length + 1];
        System.arraycopy(actions, 0, newActions, 0, actions.length);
        newActions[actions.length] = new QuarkusDevActionGroup();
        return newActions;
    }

    @Override
    public AnAction[] postProcessPopupActions(@NotNull ConsoleView console, AnAction[] actions) {
        return postProcess(console, actions);
    }
}
