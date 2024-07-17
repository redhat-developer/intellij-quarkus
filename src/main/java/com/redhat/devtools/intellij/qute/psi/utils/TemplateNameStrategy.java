/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.utils;

/**
 * Qute template name strategy managed with {@link CheckedTemplate#defaultName}/
 */
public enum TemplateNameStrategy {

	ELEMENT_NAME, //
	HYPHENATED_ELEMENT_NAME, //
	UNDERSCORED_ELEMENT_NAME
}
