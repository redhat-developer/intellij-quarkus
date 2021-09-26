/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.hover;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.eclipse.lsp4j.Hover;

/**
 * Java hover participants API.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/hover/IJavaHoverParticipant.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/hover/IJavaHoverParticipant.java</a>
 *
 */
public interface IJavaHoverParticipant {

	public static final ExtensionPointName<IJavaHoverParticipant> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.javaHoverParticipant");

	/**
	 * Returns true if hover must be collected for the given context and false
	 * otherwise.
	 * 
	 * <p>
	 * Collection is done by default. Participants can override this to check if
	 * some classes are on the classpath before deciding to process the collection.
	 * </p>
	 * 
	 * @param context the java hover context
	 * @return true if hover must be collected for the given context and false
	 *         otherwise.
	 */
	default boolean isAdaptedForHover(JavaHoverContext context) {
		return true;
	}

	/**
	 * Begin hover collection.
	 * 
	 * @param context the java hover context
	 */
	default void beginHover(JavaHoverContext context) {

	}

	/**
	 * Collect hover according to the context.
	 * 
	 * @param context the java hover context
	 *
	 * @return the hover and null otherwise.
	 */
	Hover collectHover(JavaHoverContext context);

	/**
	 * End hover collection.
	 * 
	 * @param context the java hover context
	 */
	default void endHover(JavaHoverContext context) {

	}
}
