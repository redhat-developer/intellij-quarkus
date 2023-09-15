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
package com.redhat.devtools.intellij.quarkus.psi.internal.builditems;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaErrorCode;

/**
 * Represents error codes for validation issues in classes inheriting <code>io.quarkus.builder.item.BuildItem</code>.
 */
public enum QuarkusBuildItemErrorCode implements IJavaErrorCode {

    InvalidModifierBuildItem;

    @Override
    public String getCode() {
        return name();
    }
}

