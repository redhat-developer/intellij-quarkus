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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;

/**
 * MicroProfile Config constants
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/MicroProfileConfigConstants.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/MicroProfileConfigConstants.java</a>
 *
 */
public class MicroProfileConfigConstants {

	private MicroProfileConfigConstants() {
	}

	public static final String MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE = "microprofile-config";

	public static final String INJECT_JAVAX_ANNOTATION = "javax.inject.Inject";
	public static final String INJECT_JAKARTA_ANNOTATION = "jakarta.inject.Inject";

	// @ConfigProperty annotation

	public static final String CONFIG_PROPERTY_ANNOTATION = "org.eclipse.microprofile.config.inject.ConfigProperty";

	public static final String CONFIG_PROPERTY_ANNOTATION_NAME = "name";

	public static final String CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE = "defaultValue";

	// @ConfigProperties annotation

	public static final String CONFIG_PROPERTIES_ANNOTATION = "org.eclipse.microprofile.config.inject.ConfigProperties";

	public static final String CONFIG_PROPERTIES_ANNOTATION_PREFIX = "prefix";

	public static final String CONFIG_PROPERTIES_ANNOTATION_UNCONFIGURED_PREFIX = "org.eclipse.microprofile.config.inject.configproperties.unconfiguredprefix";

	// @Asynchronous annotation

	public static final String FUTURE_TYPE_UTILITY = "java.util.concurrent.Future";

	public static final String COMPLETION_STAGE_TYPE_UTILITY = "java.util.concurrent.CompletionStage";

	// Diagnostic data

	public static final String DIAGNOSTIC_DATA_NAME = "name";

	public static final String UNI_TYPE_UTILITY = "io.smallrye.mutiny.Uni";
}
