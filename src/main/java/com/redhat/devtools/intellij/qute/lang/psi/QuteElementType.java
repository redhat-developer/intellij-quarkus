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
package com.redhat.devtools.intellij.qute.lang.psi;

import com.intellij.psi.tree.IElementType;
import com.redhat.devtools.intellij.qute.lang.QuteLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class QuteElementType extends IElementType {
    public QuteElementType(@NonNls @NotNull String debugName) {
        super(debugName, QuteLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "QuteElementType." + super.toString();
    }
}
