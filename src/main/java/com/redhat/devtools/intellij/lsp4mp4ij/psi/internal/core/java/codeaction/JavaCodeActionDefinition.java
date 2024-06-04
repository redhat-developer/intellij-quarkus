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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.java.codeaction;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.RequiredElement;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.serviceContainer.BaseKeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper class around {@link IJavaCodeActionParticipant} participants.
 */
public class JavaCodeActionDefinition extends BaseKeyedLazyInstance<IJavaCodeActionParticipant>
		implements IJavaCodeActionParticipant {

	public static final ExtensionPointName<JavaCodeActionDefinition> EP = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.javaCodeActionParticipant");

	private static final Logger LOGGER = Logger.getLogger(JavaCodeActionDefinition.class.getName());

	@Attribute("kind")
	public String kind;

	@Attribute("targetDiagnostic")
	public String targetDiagnostic;

	@Attribute("implementationClass")
	public String implementationClass;

	@Override
	public String getParticipantId() {
		return getInstance().getParticipantId();
	}

	@Override
	public boolean isAdaptedForCodeAction(JavaCodeActionContext context) {
		try {
			return getInstance().isAdaptedForCodeAction(context);
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while calling isAdaptedForCodeAction", e);
			return false;
		}
	}

	@Override
	public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
		try {
			List<? extends CodeAction> codeActions = getInstance().getCodeActions(context, diagnostic);
			return codeActions != null ? codeActions : Collections.emptyList();
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while calling getCodeActions", e);
			return Collections.emptyList();
		}
	}

	@Override
	public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
		return getInstance().resolveCodeAction(context);
	}


	/**
	 * Returns the code action kind.
	 *
	 * @return the code action kind.
	 */
	public String getKind() {
		return !StringUtils.isEmpty(kind) ? kind : CodeActionKind.QuickFix;
	}

	/**
	 * Returns the target diagnostic and null otherwise.
	 *
	 * @return the target diagnostic and null otherwise.
	 */
	public String getTargetDiagnostic() {
		return targetDiagnostic;
	}

	@Override
	protected @Nullable String getImplementationClassName() {
		return implementationClass;
	}
}
