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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs;

/**
 * Represents the HTTP methods that have specified semantics.
 */
public enum HttpMethod {
	GET, //
	HEAD, //
	POST, //
	PUT, //
	DELETE, //
	CONNECT, //
	OPTIONS, //
	TRACE, //
	PATCH;

	@Override
	public String toString() {
		return name();
	}
}