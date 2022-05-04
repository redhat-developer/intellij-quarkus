/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.java;


import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaErrorCode;

/**
 * Represents the error code for an error related to configuration properties
 * generated in a Java file.
 *
 */
public enum MicroProfileConfigErrorCode implements IJavaErrorCode {

	NO_VALUE_ASSIGNED_TO_PROPERTY, DEFAULT_VALUE_IS_WRONG_TYPE;

	@Override
	public String getCode() {
		return name();
	}

}
