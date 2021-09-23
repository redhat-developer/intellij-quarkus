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
package com.redhat.devtools.intellij.quarkus.mp4ij.psi.core.java.definition;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.redhat.devtools.intellij.quarkus.search.core.java.hover.IJavaHoverParticipant;
import org.eclipse.lsp4mp.commons.MicroProfileDefinition;

import java.util.List;

/**
 * Java definition participants API.
 *
 * @author Angelo ZERR
 *
 */
public interface IJavaDefinitionParticipant {
	public static final ExtensionPointName<IJavaDefinitionParticipant> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.javaDefinitionParticipant");

	/**
	 * Returns true if definition must be collected for the given context and false
	 * otherwise.
	 *
	 * <p>
	 * Collection is done by default. Participants can override this to check if
	 * some classes are on the classpath before deciding to process the collection.
	 * </p>
	 *
	 * @param context the java definition context
	 * @return true if definition must be collected for the given context and false
	 *         otherwise.
	 */
	default boolean isAdaptedForDefinition(JavaDefinitionContext context) {
		return true;
	}

	/**
	 * Begin definition collection.
	 *
	 * @param context the java definition context
	 */
	default void beginDefinition(JavaDefinitionContext context) {

	}

	/**
	 * Collect definition according to the context.
	 *
	 * @param context the java definition context
	 *
	 * @return the definition and null otherwise.
	 */
	List<MicroProfileDefinition> collectDefinitions(JavaDefinitionContext context);

	/**
	 * End definition collection.
	 *
	 * @param context the java definition context
	 */
	default void endDefinition(JavaDefinitionContext context) {

	}
}
