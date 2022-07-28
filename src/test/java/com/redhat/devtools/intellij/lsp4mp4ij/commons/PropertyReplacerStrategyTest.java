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
 * Contributors: Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.commons;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.PropertyReplacerStrategy.BRACKET_REPLACER;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.PropertyReplacerStrategy.EXPRESSION_REPLACER;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.PropertyReplacerStrategy.NULL_REPLACER;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for completion in Java files
 *
 * @author AlexXuChen
 */
public class PropertyReplacerStrategyTest {
	@Test
	public void nullReplacerTest() {
		String input = "${expression.value}";
		String actual = NULL_REPLACER.apply(input);
		String expected = "${expression.value}";
		Assert.assertEquals(actual, expected);
	}

	@Test
	public void bracketReplacerTest() {
		String input = "${expression.value}";
		String actual = BRACKET_REPLACER.apply(input);
		String expected = "$expression.value";
		Assert.assertEquals(actual, expected);
	}

	@Test
	public void expressionReplacerTest() {
		String input = "${expression.value}";
		String actual = EXPRESSION_REPLACER.apply(input);
		String expected = "expression.value";
		Assert.assertEquals(actual, expected);
	}
}