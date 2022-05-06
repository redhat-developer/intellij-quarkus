/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.java;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaErrorCode;

public enum MicroProfileFaultToleranceErrorCode implements IJavaErrorCode {

	FALLBACK_METHOD_DOES_NOT_EXIST,
	FAULT_TOLERANCE_DEFINITION_EXCEPTION,
	DELAY_EXCEEDS_MAX_DURATION;

	@Override
	public String getCode() {
		return name();
	}

}
