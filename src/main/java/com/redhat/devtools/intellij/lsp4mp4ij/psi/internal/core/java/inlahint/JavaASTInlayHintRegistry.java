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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.java.inlahint;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.inlayhint.JavaASTInlayHint;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.inlayhint.JavaInlayHintsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry to hold the Extension point
 * "org.eclipse.lsp4mp.jdt.core.javaASTInlayHints".
 *
 * @author Angelo ZERR
 *
 */
public class JavaASTInlayHintRegistry {

	private static final Logger LOGGER = Logger.getLogger(JavaASTInlayHintRegistry.class.getName());

	private static final JavaASTInlayHintRegistry INSTANCE = new JavaASTInlayHintRegistry();

	private static final String EXTENSION_ID = "javaASTInlayHints";

	public static JavaASTInlayHintRegistry getInstance() {
		return INSTANCE;
	}

	private boolean extensionProvidersLoaded;
	private boolean registryListenerIntialized;

	private final List<JavaASTInlayHintExtensionPointBean> inlayHintsFromClass;

	private JavaASTInlayHintRegistry() {
		super();
		this.extensionProvidersLoaded = false;
		this.registryListenerIntialized = false;
		this.inlayHintsFromClass = new ArrayList<>();
	}

	public String getExtensionId() {
		return EXTENSION_ID;
	}

	private synchronized void loadExtensionJavaASTInlayHints() {
		if (extensionProvidersLoaded)
			return;

		// Immediately set the flag, as to ensure that this method is never
		// called twice
		extensionProvidersLoaded = true;

		LOGGER.log(Level.INFO, "->- Loading ." + getExtensionId() + " extension point ->-");

		addExtensionJavaASTInlayHints();

		LOGGER.log(Level.INFO, "-<- Done loading ." + getExtensionId() + " extension point -<-");
	}

	private void addExtensionJavaASTInlayHints() {
		inlayHintsFromClass.addAll(JavaASTInlayHint.EP_NAME.getExtensionList());
	}

	public Collection<JavaRecursiveElementVisitor> getInlayHints(@NotNull JavaInlayHintsContext context) {
		loadExtensionJavaASTInlayHints();
		List<JavaRecursiveElementVisitor> inlayHints = new ArrayList<>();
		for (JavaASTInlayHintExtensionPointBean ce : inlayHintsFromClass) {
			try {
				addInlayHint(ce.createInlayHint(), context, inlayHints);
			} catch (ClassNotFoundException | NoSuchMethodException |
					 InvocationTargetException | InstantiationException | IllegalAccessException e) {
				LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
			}
		}
		return inlayHints;
	}

	private void addInlayHint(@NotNull JavaASTInlayHint inlayHint,
							  @NotNull JavaInlayHintsContext context,
							  @NotNull List<JavaRecursiveElementVisitor> inlayHints) {
		inlayHint.initialize(context);
		if (inlayHint.isAdaptedForInlayHints(context)) {
			inlayHints.add(inlayHint);
		}
	}

}