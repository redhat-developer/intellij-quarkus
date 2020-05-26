/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.internal.health;

/**
 * MicroProfile Health constants
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/health/MicroProfileHealthConstants.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/health/MicroProfileHealthConstants.java</a>
 *
 */
public class MicroProfileHealthConstants {

	private MicroProfileHealthConstants() {
	}

	public static final String HEALTH_ANNOTATION = "org.eclipse.microprofile.health.Health";
	public static final String READINESS_ANNOTATION = "org.eclipse.microprofile.health.Readiness";
	public static final String LIVENESS_ANNOTATION = "org.eclipse.microprofile.health.Liveness";

	public static final String HEALTH_CHECK_INTERFACE_NAME = "HealthCheck";

	public static final String HEALTH_CHECK_INTERFACE = "org.eclipse.microprofile.health."
			+ HEALTH_CHECK_INTERFACE_NAME;

	public static final String DIAGNOSTIC_SOURCE = "microprofile-health";
}
