/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.qute.psi.core.inspections;

import com.redhat.devtools.lsp4ij.inspections.AbstractDelegateInspection;

/**
 * Dummy inspection for undefined section tag in Qute templates
 */
public class QuteUndefinedSectionTagInspection extends AbstractDelegateInspection {
    public static final String ID = getShortName(QuteUndefinedSectionTagInspection.class.getSimpleName());
}