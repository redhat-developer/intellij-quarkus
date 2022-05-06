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

import com.intellij.openapi.extensions.ExtensionPointName;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Annotation validator registry which hosts rules for attribute value of
 * annotation.
 * 
 * @author Angelo ZERR
 *
 */
public class AnnotationValidator {

	public static final ExtensionPointName<AnnotationRuleExtensionPointBean> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.javaASTValidator.annotationValidator");

	private final Map<String /* annotation name */, AnnotationRule> rulesByAnnotation;

	public AnnotationValidator() {
		this.rulesByAnnotation = new HashMap<>();
	}

	/**
	 * Register rules validation for attributes of a given annotation.
	 * 
	 * @param newRule the annotation rule.
	 */
	public void registerRule(AnnotationRule newRule) {
		String annotation = newRule.getAnnotation();
		AnnotationRule existingRule = this.rulesByAnnotation.get(annotation);
		if (existingRule == null) {
			this.rulesByAnnotation.put(annotation, newRule);
		} else {
			// Merge rule
			newRule.getRules().forEach(attributeRule -> {
				if (!existingRule.getRules().contains(attributeRule)) {
					existingRule.getRules().add(attributeRule);
				}
			});
		}
	}

	/**
	 * Unregister annotation rule.
	 * 
	 * @param rule the annotation rule to unregister.
	 */
	public void unregisterRule(AnnotationRule rule) {
		String annotation = rule.getAnnotation();
		AnnotationRule existingRule = this.rulesByAnnotation.get(annotation);
		if (existingRule != null) {
			// Remove rule
			rule.getRules().forEach(attributeRule -> {
				existingRule.getRules().remove(attributeRule);
			});
		}
	}

	/**
	 * Validate the give attribute <code>value</code> by using the annotation rule.
	 * 
	 * @param value the attribute vale to validate.
	 * @param rule  the annotation rule to use for validate the value.
	 * 
	 * @return the error message of the validation result of the attribute value and null otherwise.
	 */
	public String validate(String value, AnnotationAttributeRule rule) {
		if (rule == null) {
			return null;
		}
		return rule.validate(value);
	}

	/**
	 * Returns the registered annotation rules.
	 * 
	 * @return the collection of registered annotation rules.
	 */
	public Collection<AnnotationRule> getRules() {
		return rulesByAnnotation.values();
	}
}
