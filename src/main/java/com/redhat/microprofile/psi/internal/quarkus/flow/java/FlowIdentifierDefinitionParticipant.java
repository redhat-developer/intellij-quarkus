/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
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
package com.redhat.microprofile.psi.internal.quarkus.flow.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiVariable;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.definition.AbstractAnnotationDefinitionParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.definition.JavaDefinitionContext;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.microprofile.psi.internal.quarkus.flow.FlowConstants;
import com.redhat.microprofile.psi.internal.quarkus.flow.PsiFlowUtils;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.MicroProfileDefinition;

import java.util.Collections;
import java.util.List;

/**
 * Quarkus Flow definition participant to navigate from an {@code @Identifier("...")} value on a
 * {@code Flow}/{@code WorkflowDefinition}-typed field, parameter or local variable to the
 * matching workflow file under {@code src/main/resources/flow}:
 *
 * <pre>
 * &#64;Inject
 * &#64;Identifier("flow:for-task-name-collision")
 * Flow exprFor;
 * </pre>
 *
 * @see <a href="https://docs.quarkiverse.io/quarkus-flow/dev/workflow-definitions.html">Quarkus Flow workflow definitions</a>
 */
public class FlowIdentifierDefinitionParticipant extends AbstractAnnotationDefinitionParticipant {

	public FlowIdentifierDefinitionParticipant() {
		super(FlowConstants.IDENTIFIER_ANNOTATION, new String[] { FlowConstants.VALUE_ANNOTATION_MEMBER });
	}

	@Override
	protected boolean isAdaptableFor(PsiElement hyperlinkedElement) {
		return (hyperlinkedElement instanceof PsiField || hyperlinkedElement instanceof PsiParameter
                || hyperlinkedElement instanceof PsiLocalVariable)
                && FlowConstants.isFlowType(((PsiVariable) hyperlinkedElement).getType());
	}

	@Override
	protected List<MicroProfileDefinition> collectDefinitions(String identifier, Range identifierRange,
			PsiAnnotation annotation, JavaDefinitionContext context) {
		Module javaProject = context.getJavaProject();
		if (javaProject == null) {
			return List.of();
		}
		VirtualFile workflowFile = PsiFlowUtils.findWorkflowFile(javaProject, identifier);
		if (workflowFile == null) {
			return List.of();
		}

		MicroProfileDefinition definition = new MicroProfileDefinition();
		LocationLink location = new LocationLink();
		definition.setLocation(location);
		location.setTargetUri(LSPIJUtils.toUriAsString(workflowFile));
		// The workflow identifier isn't tied to a specific position inside the YAML/JSON file,
		// so point at its start.
		Range targetRange = new Range(new Position(0, 0), new Position(0, 0));
		location.setTargetRange(targetRange);
		location.setTargetSelectionRange(targetRange);
		location.setOriginSelectionRange(identifierRange);

		return Collections.singletonList(definition);
	}
}
