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
package com.redhat.devtools.intellij.quarkus.search.internal.health.java;

import com.redhat.devtools.intellij.quarkus.search.core.java.diagnostics.IJavaErrorCode;

/**
 * MicroProfile Health diagnostics error code.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/health/java/MicroProfileHealthErrorCode.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/health/java/MicroProfileHealthErrorCode.java</a>
 *
 */
public enum MicroProfileHealthErrorCode implements IJavaErrorCode {

	ImplementHealthCheck, HealthAnnotationMissing;

	@Override
	public String getCode() {
		return name();
	}

}
