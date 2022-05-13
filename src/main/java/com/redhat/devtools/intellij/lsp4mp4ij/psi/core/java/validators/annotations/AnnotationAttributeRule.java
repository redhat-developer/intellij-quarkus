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

import org.eclipse.lsp4j.Diagnostic;

/**
 * The annotation attribute value used to validate an attribute value.
 * 
 * @author Angelo ZERR
 *
 */
public class AnnotationAttributeRule {

	private final String attribute;

	private String source;

	private RangeExpression rangeExpression;

	public AnnotationAttributeRule(String name) {
		this.attribute = name;
	}

	/**
	 * Returns the attribute name.
	 * 
	 * @return the attribute name.
	 */
	public String getAttribute() {
		return attribute;
	}

	/**
	 * Set the range expression to respect for the attribute value.
	 * 
	 * @param range the range expression to respect for the attribute value.
	 * @throws RangeExpressionException thrown when expression doesn't respect the
	 *                                  range expression syntax.
	 */
	public void setRange(String range) throws RangeExpressionException {
		this.rangeExpression = RangeExpression.parse(range);
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
	 * Set the diagnostic source to use when a LSP {@link Diagnostic} should be
	 * created and null otherwise.
	 * 
	 * @param source the diagnostic source to use when a LSP {@link Diagnostic}
	 *               should be created and null otherwise.
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * Validate the given attribute <code>value</code>.
	 * 
	 * @param value the attribute value.
	 * @return the error message of the validation result of the attribute value and
	 *         null otherwise.
	 */
	public String validate(String value) {
		if (rangeExpression != null) {
			Double valueAsDouble = Double.parseDouble(value);
			return rangeExpression.validate(valueAsDouble);
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
		result = prime * result + ((rangeExpression == null) ? 0 : rangeExpression.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AnnotationAttributeRule other = (AnnotationAttributeRule) obj;
		if (attribute == null) {
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (rangeExpression == null) {
			if (other.rangeExpression != null)
				return false;
		} else if (!rangeExpression.equals(other.rangeExpression))
			return false;
		return true;
	}

}
