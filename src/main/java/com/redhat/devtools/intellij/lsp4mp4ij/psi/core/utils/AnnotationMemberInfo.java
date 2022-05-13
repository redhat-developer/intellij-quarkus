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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils;

import org.eclipse.lsp4j.Range;

/**
 * Utility class to store the annotation member info such as the value and the
 * range
 *
 * e.g. Given annotation and hover at position |
 *
 * @ConfigProperty(name = "g|reeting.message") The value is greeting.message and
 *                      the range is 17-40
 *
 * @ConfigProperty(name = "greeting.message", defaultValue = "he|llo") The value
 *                      is hello and the range is 43-65
 *
 * @author AlexXuChen
 *
 */
public class AnnotationMemberInfo {

	private final String memberValue;

	private final Range range;

	public AnnotationMemberInfo(String memberValue, Range range) {
		this.memberValue = memberValue;
		this.range = range;
	}

	/**
	 * Returns the annotation member value
	 *
	 * @return the annotation member value
	 */
	public String getMemberValue() {
		return memberValue;
	}

	/**
	 * Returns the annotation member range
	 *
	 * @return the annotation member range
	 */
	public Range getRange() {
		return range;
	}
}