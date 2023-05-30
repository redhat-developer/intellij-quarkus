/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
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

import org.apache.commons.lang3.StringUtils;

/**
 * Template path information which stores :
 * 
 * <ul>
 * <li>the template Uri</li>
 * <li>the fragment if or null</li>
 * </ul>
 * 
 * @author Angelo ZERR
 *
 */
public class TemplatePathInfo {

	private final String templateUri;

	private final String fragmentId;

	public TemplatePathInfo(String templateUri, String fragmentId) {
		this.templateUri = templateUri;
		this.fragmentId = fragmentId;
	}

	/**
	 * Returns the template Uri.
	 * 
	 * @return the template Uri.
	 */
	public String getTemplateUri() {
		return templateUri;
	}

	/**
	 * Returns the fragment id and null otherwise.
	 * 
	 * @return the fragment id and null otherwise.
	 */
	public String getFragmentId() {
		return fragmentId;
	}

	/**
	 * Returns true if a fragment id is defined and false otherwise.
	 * 
	 * @return true if a fragment id is defined and false otherwise.
	 */
	public boolean hasFragment() {
		return StringUtils.isNotEmpty(fragmentId);
	}

	/**
	 * Returns true if fragment is null or non empty and false otherwise.
	 * 
	 * @return true if fragment is null or non empty and false otherwise.
	 */
	public boolean isValid() {
		// A fragment cannot be empty
		return (fragmentId == null || hasFragment());
	}

}