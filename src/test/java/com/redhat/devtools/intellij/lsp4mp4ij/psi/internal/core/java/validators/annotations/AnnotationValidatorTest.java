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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.java.validators.annotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations.AnnotationAttributeRule;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations.AnnotationValidator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations.RangeExpressionException;
import org.junit.Test;

/**
 * Test for annotation based on rules.
 * 
 * @author Angelo ZERR
 *
 */
public class AnnotationValidatorTest {

	@Test
	public void noRule() throws RangeExpressionException {
		AnnotationValidator validator = new AnnotationValidator();
		String result = validator.validate("-1", null);
		assertNull(result);
	}

	@Test
	public void testGreaterThanOrEqual() throws RangeExpressionException {
		assertValidation("0", "0", null);
		assertValidation("0", "1", null);
		assertValidation("0", "-1", "The value `-1` must be greater than or equal to `0`.");
	}

	@Test
	public void testGreaterThanOrEqual2() throws RangeExpressionException {
		assertValidation("[0", "0", null);
		assertValidation("[0", "1", null);
		assertValidation("[0", "-1", "The value `-1` must be greater than or equal to `0`.");
	}

	@Test
	public void testGreaterThanOrEqualWithNegativeValue() throws RangeExpressionException {
		assertValidation("-1", "0", null);
		assertValidation("-1", "1", null);
		assertValidation("-1", "-1", null);
		assertValidation("-1", "-2", "The value `-2` must be greater than or equal to `-1`.");
	}

	@Test
	public void testGreaterThan() throws RangeExpressionException {
		assertValidation("(0", "0.1", null);
		assertValidation("(0", "0", "The value `0` must be greater than `0`.");
		assertValidation("(0", "-1", "The value `-1` must be greater than `0`.");
	}

	@Test
	public void testBetweenInclusive() throws RangeExpressionException {
		assertValidation("[0,2]", "0", null);
		assertValidation("[0,2]", "1", null);
		assertValidation("[0,2]", "2", null);
		assertValidation("[0,2]", "-1", "The value `-1` must be between `0` (inclusive) and `2` (inclusive).");
	}

	@Test
	public void testBetweenFromExclusive() throws RangeExpressionException {
		assertValidation("(0,2]", "0", "The value `0` must be between `0` (exclusive) and `2` (inclusive).");
		assertValidation("(0,2]", "1", null);
		assertValidation("(0,2]", "2", null);
		assertValidation("(0,2]", "-1", "The value `-1` must be between `0` (exclusive) and `2` (inclusive).");
	}

	@Test
	public void testBetweenToExclusive() throws RangeExpressionException {
		assertValidation("[0,2)", "0", null);
		assertValidation("[0,2)", "1", null);
		assertValidation("[0,2)", "2", "The value `2` must be between `0` (inclusive) and `2` (exclusive).");
		assertValidation("[0,2)", "-1", "The value `-1` must be between `0` (inclusive) and `2` (exclusive).");
	}

	@Test
	public void testBetweenBothExclusive() throws RangeExpressionException {
		assertValidation("(0,2)", "0", "The value `0` must be between `0` (exclusive) and `2` (exclusive).");
		assertValidation("(0,2)", "1", null);
		assertValidation("(0,2)", "2", "The value `2` must be between `0` (exclusive) and `2` (exclusive).");
		assertValidation("(0,2)", "-1", "The value `-1` must be between `0` (exclusive) and `2` (exclusive).");
	}

	private static void assertValidation(String range, String value, String errorMessage)
			throws RangeExpressionException {
		AnnotationValidator validator = new AnnotationValidator();
		AnnotationAttributeRule rule = new AnnotationAttributeRule("foo");
		rule.setRange(range);

		String result = validator.validate(value, rule);
		assertEquals(errorMessage, result);
	}
}
