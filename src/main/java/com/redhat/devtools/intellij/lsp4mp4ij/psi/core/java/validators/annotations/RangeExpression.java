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

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;

/**
 * The range expression used to validate value from an attribute annotation.
 * 
 * The range is specified like OSGi version syntax:
 * 
 * <ul>
 * <li>0 -> means >=0</li>
 * <li>[0 -> means >=0</li>
 * <li>(0 -> means >0</li>
 * <li>[0,1] -> means >=0 && <=1</li>
 * <li>(0,1] -> means >0 && <=1</li>
 * <li>(0,1) -> means >0 && <1</li>
 * <li>[0,1) -> means >=0 && <1</li>
 * </ul>
 * 
 * @author Angelo ZERR
 *
 */
public class RangeExpression {

	private final Edge from;
	private final Edge to;
	private final String errorMessage;

	public RangeExpression(Edge from, Edge to) {
		this.from = from;
		this.to = to;
		this.errorMessage = createErrorMessage(from, to);
	}

	private static String createErrorMessage(Edge from, Edge to) {
		StringBuilder message = new StringBuilder("The value `{0}`' must be ");
		if (to == null) {
			message.append("greater than ");
			if (from.inclusive) {
				message.append("or equal to ");
			}
			message.append("`");
			message.append(from.valueAsString);
			message.append("`.");
		} else {
			// The value must be between 0 and 1 inclusive.
			message.append("between ");
			message.append("`");
			message.append(from.valueAsString);
			message.append("`");
			if (from.inclusive) {
				message.append(" (inclusive)");
			} else {
				message.append(" (exclusive)");
			}
			message.append(" and ");
			message.append("`");
			message.append(to.valueAsString);
			message.append("`");
			if (to.inclusive) {
				message.append(" (inclusive)");
			} else {
				message.append(" (exclusive)");
			}
			message.append(".");
		}
		return message.toString();
	}

	public String validate(double value) {
		if (!from.validate(value) || (to != null && !to.validate(value))) {
			return MessageFormat.format(errorMessage, value);
		}
		return null;
	}

	/**
	 * Parse the given range <code>expression</code>.
	 * 
	 * @param expression the range expression to parse.
	 * @return an instance of range expression.
	 * @throws RangeExpressionException thrown when expression doesn't respect the
	 *                                  range expression syntax.
	 */
	public static RangeExpression parse(String expression) throws RangeExpressionException {
		if (StringUtils.isEmpty(expression)) {
			throw new RangeExpressionException("No expression");
		}
		Edge from = null;
		Edge to = null;

		boolean inclusive = true;
		StringBuilder valueAsString = new StringBuilder();

		for (int i = 0; i < expression.length(); i++) {
			char c = expression.charAt(i);
			switch (c) {
			case ' ':
				// ignore whitespace
				break;
			case '[':
				if (from != null && valueAsString.length() > 0) {
					unexpectedToken(c, i, expression);
				}
				inclusive = true;
				break;
			case '(':
				if (from != null && valueAsString.length() > 0) {
					unexpectedToken(c, i, expression);
				}
				inclusive = false;
				break;
			case ']':
				if (from == null) {
					unexpectedToken(c, i, expression);
				}
				inclusive = true;
				break;
			case ')':
				if (from == null) {
					unexpectedToken(c, i, expression);
				}
				inclusive = false;
				break;
			case ',':
				if (valueAsString.length() == 0) {
					unexpectedToken(c, i, expression);
				}
				from = createEdge(inclusive, true, valueAsString.toString());
				valueAsString.setLength(0);
				inclusive = true;
				break;
			case '.':
				if (valueAsString.length() == 0 || valueAsString.toString().contains(c + "")) {
					unexpectedToken(c, i, expression);
				}
				valueAsString.append(c);
				break;
			default:
				if (Character.isDigit(c)) {
					valueAsString.append(c);
				} else {
					unexpectedToken(c, i, expression);
				}
			}
		}
		if (valueAsString.length() > 0) {
			if (from == null) {
				from = createEdge(inclusive, true, valueAsString.toString());
			} else {
				to = createEdge(inclusive, false, valueAsString.toString());
			}
		}
		if (from == null) {
			throw new RangeExpressionException("No range");
		}
		return new RangeExpression(from, to);
	}

	private static Edge createEdge(boolean inclusive, boolean superior, String valueAsString)
			throws RangeExpressionException {
		if (valueAsString.length() == 0) {
			throw new RangeExpressionException("No value");
		}
		return new Edge(valueAsString, superior, inclusive);
	}

	private static void unexpectedToken(char c, int i, String expression) throws RangeExpressionException {
		throw new RangeExpressionException("Unexpected character " + c + " at " + i + " for expression " + expression);
	}

	private static class Edge {

		private final double value;

		private final String valueAsString;

		private final boolean superior;
		private final boolean inclusive;

		public Edge(String valueAsString, boolean superior, boolean inclusive) {
			this.valueAsString = valueAsString;
			this.value = Double.parseDouble(valueAsString);
			this.superior = superior;
			this.inclusive = inclusive;
		}

		public boolean validate(double value) {
			if (inclusive) {
				if (value == this.value) {
					return true;
				}
			}
			if (superior) {
				if (value > this.value) {
					return true;
				}
			} else if (value < this.value) {
				return true;
			}
			return false;
		}
	}

}
