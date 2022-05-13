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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;

/**
 * The container of attribute rules for a given annotation.
 * 
 * @author Angelo ZERR
 *
 */
public class AnnotationRule {

	private final String annotation;

	private final List<AnnotationAttributeRule> rules;

	private final String source;

	public AnnotationRule(String annotation) {
		this(annotation, null);
	}

	public AnnotationRule(String annotation, String source) {
		this.annotation = annotation;
		this.source = source;
		this.rules = new ArrayList<>();
	}

	/**
	 * Returns the annotation name.
	 * 
	 * @return the annotation name.
	 */
	public String getAnnotation() {
		return annotation;
	}

	/**
	 * Returns the diagnostic source to use when a LSP {@link Diagnostic} should be
	 * created and null otherwise.
	 * 
	 * @return the diagnostic source to use when a LSP {@link Diagnostic} should be
	 *         created and null otherwise.
	 */
	public String getSource() {
		return source;
	}

	/**
	 * Add an attribute rule validation.
	 * 
	 * @param attributeRule the attribute value rule to add.
	 */
	public void addRule(AnnotationAttributeRule attributeRule) {
		attributeRule.setSource(getSource());
		this.rules.add(attributeRule);
	}

	/**
	 * Returns the list of attribute value rule to use for the annotation.
	 * 
	 * @return the list of attribute value rule to use for the annotation.
	 */
	public List<AnnotationAttributeRule> getRules() {
		return rules;
	}
}
