/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.internal.restclient;

import com.redhat.devtools.intellij.quarkus.search.core.java.diagnostics.IJavaErrorCode;

/**
 * MicroProfile RestClient diagnostics error code.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/restclient/MicroProfileRestClientErrorCode.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/restclient/MicroProfileRestClientErrorCode.java</a>
 *
 */
public enum MicroProfileRestClientErrorCode implements IJavaErrorCode {

	RegisterRestClientAnnotationMissing, InjectAnnotationMissing, RestClientAnnotationMissing, InjectAndRestClientAnnotationMissing;

	@Override
	public String getCode() {
		return name();
	}

}
