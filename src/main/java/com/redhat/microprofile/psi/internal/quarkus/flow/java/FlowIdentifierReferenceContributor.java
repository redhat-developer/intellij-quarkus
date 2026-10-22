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
package com.redhat.microprofile.psi.internal.quarkus.flow.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.microprofile.psi.internal.quarkus.flow.FlowConstants;
import com.redhat.microprofile.psi.internal.quarkus.flow.PsiFlowUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers a native {@link PsiReference} for the string literal value of
 * {@code @Identifier("...")} on a {@code Flow}/{@code WorkflowDefinition}-typed
 * field/parameter/local variable, so that Ctrl/Cmd-click (Go to Declaration) navigates straight
 * to the matching Quarkus Flow workflow file under {@code src/main/resources/flow}:
 *
 * <pre>
 * &#64;Inject
 * &#64;Identifier("flow:for-task-name-collision")
 * Flow exprFor;
 * </pre>
 *
 * <p>
 * This resolves entirely in-process via PSI - no language server round-trip - which is the
 * standard IntelliJ mechanism for this kind of "string literal references another file"
 * navigation.
 * </p>
 *
 * @see <a href="https://docs.quarkiverse.io/quarkus-flow/dev/workflow-definitions.html">Quarkus Flow workflow definitions</a>
 */
public class FlowIdentifierReferenceContributor extends PsiReferenceContributor {

	private static final String VALUE_MEMBER = "value";

	@Override
	public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
		registrar.registerReferenceProvider(PlatformPatterns.psiElement(PsiLiteralExpression.class),
				new FlowIdentifierReferenceProvider());
	}

	private static final class FlowIdentifierReferenceProvider extends PsiReferenceProvider {

		@Override
		public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
				@NotNull ProcessingContext context) {
			if (!(element instanceof PsiLiteralExpression literal) || !(literal.getValue() instanceof String identifier)) {
				return PsiReference.EMPTY_ARRAY;
			}

			PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
			if (annotation == null || !FlowConstants.IDENTIFIER_ANNOTATION.equals(annotation.getQualifiedName())) {
				return PsiReference.EMPTY_ARRAY;
			}

			// Only the member actually holding this literal (handles both the explicit
			// `@Identifier(value = "...")` and shorthand `@Identifier("...")` forms).
			PsiAnnotationMemberValue valueExpression = AnnotationUtils.getAnnotationMemberValueExpression(annotation,
					VALUE_MEMBER);
			if (valueExpression != element) {
				return PsiReference.EMPTY_ARRAY;
			}

			PsiElement owner = PsiTreeUtil.getParentOfType(annotation, PsiField.class, PsiParameter.class,
					PsiLocalVariable.class);
			if (!isFlowInjectionPoint(owner)) {
				return PsiReference.EMPTY_ARRAY;
			}

			return new PsiReference[] { new FlowWorkflowReference(literal, identifier) };
		}

		private static boolean isFlowInjectionPoint(PsiElement element) {
			if (element instanceof PsiField || element instanceof PsiParameter || element instanceof PsiLocalVariable) {
				return isFlowType(((PsiVariable) element).getType());
			}
			return false;
		}

		/**
		 * The injected bean is typically a {@code Flow}, but Quarkus Flow also allows injecting
		 * the lower-level {@code WorkflowDefinition} directly - both are keyed by the same
		 * {@code @Identifier} value.
		 */
		private static boolean isFlowType(PsiType type) {
			if (type instanceof PsiClassType classType) {
				PsiClass clazz = classType.resolve();
				if (clazz == null) {
					return false;
				}
				String qualifiedName = clazz.getQualifiedName();
				return FlowConstants.FLOW_CLASS.equals(qualifiedName)
						|| FlowConstants.WORKFLOW_DEFINITION_CLASS.equals(qualifiedName);
			}
			return false;
		}
	}

	private static final class FlowWorkflowReference extends PsiReferenceBase<PsiLiteralExpression> {

		private final String identifier;

		FlowWorkflowReference(PsiLiteralExpression element, String identifier) {
			super(element, unquotedRange(element));
			this.identifier = identifier;
		}

		@Override
		public @Nullable PsiElement resolve() {
			Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
			if (module == null) {
				return null;
			}
			VirtualFile workflowFile = PsiFlowUtils.findWorkflowFile(module, identifier);
			if (workflowFile == null) {
				return null;
			}
			return PsiManager.getInstance(getElement().getProject()).findFile(workflowFile);
		}

		@Override
		public Object @NotNull [] getVariants() {
			return EMPTY_ARRAY;
		}

		/**
		 * Returns the range of the literal's text content, excluding the surrounding double quotes.
		 */
		private static TextRange unquotedRange(PsiLiteralExpression element) {
			String text = element.getText();
			if (text.length() >= 2 && text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"') {
				return new TextRange(1, text.length() - 1);
			}
			return TextRange.allOf(text);
		}
	}

}
