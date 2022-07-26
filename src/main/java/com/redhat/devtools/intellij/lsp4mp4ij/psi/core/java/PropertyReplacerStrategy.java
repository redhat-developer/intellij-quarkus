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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java;

import java.util.function.Function;

/**
 *
 * Utility class for replacing property expressions
 *
 */
public class PropertyReplacerStrategy {

	public static final Function<String, String> NULL_REPLACER = propertyKey -> propertyKey;

	public static final Function<String, String> BRACKET_REPLACER = propertyKey -> propertyKey.replace("{", "")
			.replace("}", "");

	public static final Function<String, String> EXPRESSION_REPLACER = propertyKey -> propertyKey.replace("{", "")
			.replace("}", "").replace("$", "");

}