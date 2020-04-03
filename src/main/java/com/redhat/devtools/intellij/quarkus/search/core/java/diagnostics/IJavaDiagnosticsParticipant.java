/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.core.java.diagnostics;

import java.util.List;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.eclipse.lsp4j.Diagnostic;

/**
 * Java diagnostics participants API.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/diagnostics/IJavaDiagnosticsParticipant.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/diagnostics/IJavaDiagnosticsParticipant.java</a>
 *
 */
public interface IJavaDiagnosticsParticipant {

	public static final ExtensionPointName<IJavaDiagnosticsParticipant> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.javaDiagnosticsParticipant");

	/**
	 * Returns true if diagnostics must be collected for the given context and false
	 * otherwise.
	 * 
	 * <p>
	 * Collection is done by default. Participants can override this to check if
	 * some classes are on the classpath before deciding to process the collection.
	 * </p>
	 * 
	 * @param context the java diagnostics context
	 * @return true if diagnostics must be collected for the given context and false
	 *         otherwise.
	 * 
	 */
	default boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
		return true;
	}

	/**
	 * Begin diagnostics collection.
	 * 
	 * @param context the java diagnostics context
	 *
	 */
	default void beginDiagnostics(JavaDiagnosticsContext context) {

	}

	/**
	 * Collect diagnostics according to the context.
	 * 
	 * @param context the java diagnostics context
	 *
	 * @return diagnostics list and null otherwise.
	 * 
	 */
	List<Diagnostic> collectDiagnostics(JavaDiagnosticsContext context);

	/**
	 * End diagnostics collection.
	 * 
	 * @param context the java diagnostics context
	 *
	 */
	default void endDiagnostics(JavaDiagnosticsContext context) {

	}
}
