/*******************************************************************************
* Copyright (c) 2025 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.inlayhint;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProgressIndicator;
import org.eclipse.lsp4j.InlayHint;

import java.util.List;

/**
 * Java inlayHint participants API.
 *
 * @author Angelo ZERR
 *
 */
public interface IJavaInlayHintsParticipant {

	ExtensionPointName<IJavaInlayHintsParticipant> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.javaInlayHintsParticipant");

	/**
	 * Returns true if inlayHint must be collected for the given context and false
	 * otherwise.
	 *
	 * <p>
	 * Collection is done by default. Participants can override this to check if
	 * some classes are on the classpath before deciding to process the collection.
	 * </p>
	 *
	 * @param context the java inlayHint context
	 * @param monitor
	 * @return true if inlayHint must be collected for the given context and false
	 *         otherwise.
	 *
	 */
	default boolean isAdaptedForInlayHint(JavaInlayHintsContext context, ProgressIndicator monitor) {
		return true;
	}

	/**
	 * Begin inlayHint collection.
	 *
	 * @param context the java inlayHint context
	 * @param monitor
	 *
	 */
	default void beginInlayHint(JavaInlayHintsContext context, ProgressIndicator monitor) {

	}

	/**
	 * Collect inlayHint according to the context.
	 *
	 * @param context the java inlayHint context
	 *
	 */
	void collectInlayHint(JavaInlayHintsContext context, ProgressIndicator monitor);

	/**
	 * End inlayHint collection.
	 *
	 * @param context the java inlayHint context
	 * @param monitor
	 *
	 */
	default void endInlayHint(JavaInlayHintsContext context, ProgressIndicator monitor) {

	}
}
