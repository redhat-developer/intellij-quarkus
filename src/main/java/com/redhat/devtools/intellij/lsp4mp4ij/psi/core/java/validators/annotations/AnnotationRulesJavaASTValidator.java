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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiBinaryExpression;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiPrefixExpression;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.java.validators.JavaASTValidatorRegistry;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * JDT Java AST visitor which validate annotation attributes by using annotation
 * rules registered.
 * 
 * @author Angelo ZERR
 *
 */
public class AnnotationRulesJavaASTValidator extends JavaASTValidator {

	private static final Logger LOGGER = Logger.getLogger(AnnotationRulesJavaASTValidator.class.getName());

	private final Collection<AnnotationRule> rules;

	public AnnotationRulesJavaASTValidator(Collection<AnnotationRule> rules) {
		this.rules = rules;
	}

	@Override
	public void visitAnnotation(PsiAnnotation annotation) {
		// Loop for rules
		for (AnnotationRule annotationRule : rules) {
			if (AnnotationUtils.isMatchAnnotation(annotation, annotationRule.getAnnotation())) {
				// The AST annotation matches a rule
				List<AnnotationAttributeRule> attributeRules = annotationRule.getRules();
				// Validate attributes of the AST annotation
				for (AnnotationAttributeRule attributeRule : attributeRules) {

						PsiAnnotationMemberValue attributeValueExpr = AnnotationUtils.getAnnotationMemberValueExpression(annotation,
								attributeRule.getAttribute());
						if (attributeValueExpr != null) {
							validateAnnotationAttributeValue(attributeValueExpr, attributeRule);
						}
				}

			}
		}
	}

	/**
	 * Validate the given AST attribute value expression
	 * <code>attributeValueExpr</code> by using the given rule
	 * <code>attributeValue</code> and create a diagnostic if there is an error.
	 * 
	 * @param attributeValueExpr
	 * @param attributeRule
	 */
	private void validateAnnotationAttributeValue(PsiAnnotationMemberValue attributeValueExpr, AnnotationAttributeRule attributeRule) {
		if (attributeValueExpr == null) {
			return;
		}
		// Ensure value of AST attribute is a valid integer or an expression that can be evaluated
		if (!isInteger(attributeValueExpr) && !isInfixIntegerExpression(attributeValueExpr)) {
			return;
		}

		// Get the value of the AST attribute
		Object valueAsObject = JavaPsiFacade.getInstance(getContext().getJavaProject().getProject()).getConstantEvaluationHelper().computeConstantExpression(attributeValueExpr);
		String valueAsString = valueAsObject != null ? valueAsObject.toString() : null;
		if (StringUtils.isEmpty(valueAsString)) {
			return;
		}
		// Validate the value with the rule
		String validationResult = JavaASTValidatorRegistry.getInstance().validate(valueAsString, attributeRule);
		if (validationResult != null) {
			// There is an error, report a diagnostic
			super.addDiagnostic(validationResult, attributeRule.getSource(), attributeValueExpr, null,
					DiagnosticSeverity.Error);
		}
	}

	private static boolean isInteger(PsiAnnotationMemberValue attributeValueExpr) {
		if ((attributeValueExpr instanceof PsiLiteral && ((PsiLiteral) attributeValueExpr).getValue() instanceof Number) || (attributeValueExpr instanceof PsiPrefixExpression
				&& (((PsiPrefixExpression) attributeValueExpr).getOperationTokenType() == JavaTokenType.MINUS
				|| ((PsiPrefixExpression) attributeValueExpr).getOperationTokenType() == JavaTokenType.PLUS))) {
			return true;
		}
		return false;
	}

	private static boolean isInfixIntegerExpression(PsiAnnotationMemberValue attributeValueExpr) {
		if (attributeValueExpr instanceof PsiBinaryExpression
				&& (((PsiBinaryExpression) attributeValueExpr).getOperationTokenType() == JavaTokenType.ASTERISK
				|| ((PsiBinaryExpression) attributeValueExpr).getOperationTokenType() == JavaTokenType.DIV
				|| ((PsiBinaryExpression) attributeValueExpr).getOperationTokenType() == JavaTokenType.PERC
				|| ((PsiBinaryExpression) attributeValueExpr).getOperationTokenType() == JavaTokenType.PLUS
				|| ((PsiBinaryExpression) attributeValueExpr).getOperationTokenType() == JavaTokenType.MINUS)) {
			return true;
		}
		return false;
	}
}
