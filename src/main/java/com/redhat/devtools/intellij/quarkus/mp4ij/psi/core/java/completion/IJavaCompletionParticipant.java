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
package com.redhat.devtools.intellij.quarkus.mp4ij.psi.core.java.completion;

import java.util.List;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.eclipse.lsp4j.CompletionItem;

/**
 * The API for a completion feature
 *
 * @author datho7561
 */
public interface IJavaCompletionParticipant {
	public static final ExtensionPointName<IJavaCompletionParticipant> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.javaCompletionParticipant");

	/**
	 * Returns true if this completion feature should be active in this context, and false otherwise
	 *
	 * @param context the context of where completion is triggered
	 * @return true if this completion feature should be active in this context, and false otherwise
	 */
	default boolean isAdaptedForCompletion(JavaCompletionContext context) {
		return true;
	}

	/**
	 * Returns the completion items for the given completion context
	 *
	 * @param context the completion context
	 * @return the completion items for the given completion context
	 */
	List<? extends CompletionItem> collectCompletionItems(JavaCompletionContext context);
}
