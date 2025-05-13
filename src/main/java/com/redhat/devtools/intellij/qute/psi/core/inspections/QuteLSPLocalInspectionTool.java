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
package com.redhat.devtools.intellij.qute.psi.core.inspections;

import com.redhat.devtools.lsp4ij.inspections.LSPLocalInspectionToolBase;

/**
 * Custom LSP inspection tool for Qute to display
 * Qute templates warnings/errors in Qute/Templates tree node
 * from the problem view (after clicking on "Inspect Code..." ).
 */
public class QuteLSPLocalInspectionTool extends LSPLocalInspectionToolBase {

    public static final String ID = "QuteLSPLocalInspectionTool";
}