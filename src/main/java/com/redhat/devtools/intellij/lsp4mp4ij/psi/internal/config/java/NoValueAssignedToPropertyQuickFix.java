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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.java;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_NAME;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValue;
import static com.redhat.qute.services.diagnostics.QuteDiagnosticContants.DIAGNOSTIC_DATA_NAME;
import static org.eclipse.lsp4mp.commons.MicroProfileCodeActionFactory.createAddToUnassignedExcludedCodeAction;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.CodeActionFactory;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.IConfigSource;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * QuickFix for fixing
 * {@link MicroProfileConfigErrorCode#NO_VALUE_ASSIGNED_TO_PROPERTY} error by
 * providing several code actions:
 *
 * <ul>
 * <li>Insert the proper property inside *.properties files.</li>
 * </ul>
 * 
 * @author Angelo ZERR
 *
 */
public class NoValueAssignedToPropertyQuickFix implements IJavaCodeActionParticipant {

	private static final String CODE_ACTION_LABEL = "Insert ''{0}'' property in ''{1}''";

	@Override
	public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
		List<CodeAction> codeActions = new ArrayList<>();
		Module javaProject = context.getJavaProject();

		String lineSeparator = CodeStyleSettingsManager.getInstance(javaProject.getProject()).getMainProjectCodeStyle().
				getLineSeparator();
		if (lineSeparator == null) {
			lineSeparator = System.lineSeparator();
		}
		String propertyName = getPropertyName(diagnostic, context);
		String insertText = propertyName + "=" + lineSeparator;

		PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(javaProject.getProject()).
				getJDTMicroProfileProject(javaProject);
		List<IConfigSource> configSources = mpProject.getConfigSources();
		for (IConfigSource configSource : configSources) {
			String uri = configSource.getSourceConfigFileURI();
			if (uri != null) {
				// the properties file exists
				TextDocumentItem document = new TextDocumentItem(uri, "properties", 0, insertText);
				CodeAction codeAction = CodeActionFactory.insert(
						getTitle(propertyName, configSource.getConfigFileName()), new Position(0, 0), insertText,
						document, diagnostic);
				codeActions.add(codeAction);
			}
		}
		if (context.getParams().isCommandConfigurationUpdateSupported()) {
			// Exclude validation for the given property
			codeActions.add(createAddToUnassignedExcludedCodeAction(propertyName, diagnostic));
		}
		return codeActions;
	}

	private static String getPropertyName(Diagnostic diagnostic, JavaCodeActionContext context) {
		if (diagnostic.getData() != null) {
			// retrieve the property name from the diagnostic data
			JsonObject data = (JsonObject) diagnostic.getData();
			JsonElement name = data.get(DIAGNOSTIC_DATA_NAME);
			if (name != null) {
				return name.getAsString();
			}
		}

		// retrieve the property name from the data
		Position hoverPosition = diagnostic.getRange().getStart();
		IPsiUtils utils = context.getUtils();
		PsiFile typeRoot = context.getTypeRoot();
		int offset = utils.toOffset(typeRoot, hoverPosition.getLine(), hoverPosition.getCharacter());
		PsiElement hoverElement = typeRoot.findElementAt(offset);

		PsiAnnotation configPropertyAnnotation = getAnnotation(hoverElement, CONFIG_PROPERTY_ANNOTATION);
		return getAnnotationMemberValue(configPropertyAnnotation, CONFIG_PROPERTY_ANNOTATION_NAME);
	}

	private static String getTitle(String propertyName, String configFileName) {
		return MessageFormat.format(CODE_ACTION_LABEL, propertyName, configFileName);
	}

}
