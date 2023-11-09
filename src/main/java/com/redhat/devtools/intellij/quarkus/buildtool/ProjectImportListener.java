/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.buildtool;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Project import listener API.
 *
 */
public interface ProjectImportListener {

    /**
     * On import finished.
     *
     * @param modules imported modules.
     */
    void importFinished(@NotNull List<Module> modules);

}
