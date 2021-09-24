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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.java;


import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.hover.PropertiesHoverParticipant;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_NAME;

/**
 *
 * MicroProfile Config Hover
 * 
 * @author Angelo ZERR
 * 
 * @see <a href="https://github.com/eclipse/microprofile-config">https://github.com/eclipse/microprofile-config</a>
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/config/java/MicroProfileConfigHoverParticipant.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/config/java/MicroProfileConfigHoverParticipant.java</a>
 *
 */
public class MicroProfileConfigHoverParticipant extends PropertiesHoverParticipant {

	public MicroProfileConfigHoverParticipant() {
		super(CONFIG_PROPERTY_ANNOTATION, CONFIG_PROPERTY_ANNOTATION_NAME, CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE);
	}
}
