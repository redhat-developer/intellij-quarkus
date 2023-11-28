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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.graphql.java;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaErrorCode;

/**
 * MicroProfile GraphQL diagnostics error code.
 */
public enum MicroProfileGraphQLErrorCode implements IJavaErrorCode {

	NO_VOID_QUERIES,
	NO_VOID_MUTATIONS,
	WRONG_DIRECTIVE_PLACEMENT,
	SUBSCRIPTION_MUST_RETURN_MULTI,
	SINGLE_RESULT_OPERATION_MUST_NOT_RETURN_MULTI;

	@Override
	public String getCode() {
		return name();
	}

}