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

import com.intellij.lang.annotation.HighlightSeverity;
import org.jetbrains.annotations.NotNull;

/**
 * Problem severity levels used by LSP4MP
 */
//TODO move to lsp4ij?
public enum ProblemSeverity {
    none, warning, error;

    /**
     * Maps {@link HighlightSeverity} to {@link ProblemSeverity} levels used by LSP4MP.
     * <ul>
     *     <li>Any severity below <code>HighlightSeverity.WEAK_WARNING</code> is mapped to <code>ProblemSeverity.none</code></li>
     *     <li>Any severity below <code>HighlightSeverity.ERROR</code> is mapped to <code>ProblemSeverity.warning</code></li>
     *     <li>Any other severity is mapped to <code>ProblemSeverity.error</code></li>
     * </ul>
     *
     * @param highlightSeverity the severity to map to a {@link ProblemSeverity}
     * @return the matching {@link ProblemSeverity}
     */
    public static @NotNull ProblemSeverity getSeverity(@NotNull HighlightSeverity highlightSeverity) {
        if (HighlightSeverity.WEAK_WARNING.compareTo(highlightSeverity) > 0) {
            return none;
        }
        if (HighlightSeverity.ERROR.compareTo(highlightSeverity) > 0) {
            return warning;
        }
        return error;
    }
}
