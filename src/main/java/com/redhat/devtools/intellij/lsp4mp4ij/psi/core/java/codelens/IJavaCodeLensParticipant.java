/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProgressIndicator;
import org.eclipse.lsp4j.CodeLens;

import java.util.List;

/**
 * Java codeLens participants API.
 *
 * @author Angelo ZERR
 *
 */
public interface IJavaCodeLensParticipant {

	ExtensionPointName<IJavaCodeLensParticipant> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.javaCodeLensParticipant");


	/**
	 * Returns true if codeLens must be collected for the given context and false
	 * otherwise.
	 *
	 * <p>
	 * Collection is done by default. Participants can override this to check if
	 * some classes are on the classpath before deciding to process the collection.
	 * </p>
	 *
	 * @param context the java codeLens context
	 * @param monitor
	 * @return true if codeLens must be collected for the given context and false
	 *         otherwise.
	 *
	 */
	default boolean isAdaptedForCodeLens(JavaCodeLensContext context, ProgressIndicator monitor) {
		return true;
	}

	/**
	 * Begin codeLens collection.
	 *
	 * @param context the java codeLens context
	 * @param monitor
	 *
	 */
	default void beginCodeLens(JavaCodeLensContext context, ProgressIndicator monitor) {

	}

	/**
	 * Collect codeLens according to the context.
	 *
	 * @param context the java codeLens context
	 *
	 * @return the codeLens list and null otherwise.
	 *
	 */
	List<CodeLens> collectCodeLens(JavaCodeLensContext context, ProgressIndicator monitor);

	/**
	 * End codeLens collection.
	 *
	 * @param context the java codeLens context
	 * @param monitor
	 *
	 */
	default void endCodeLens(JavaCodeLensContext context, ProgressIndicator monitor) {

	}
}
