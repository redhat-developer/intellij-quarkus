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
package com.redhat.microprofile.psi.internal.quarkus.scheduler.java;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants;
import com.redhat.microprofile.psi.internal.quarkus.scheduler.SchedulerErrorCodes;
import com.redhat.microprofile.psi.internal.quarkus.scheduler.SchedulerUtils;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.logging.Logger;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValueExpression;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.isMatchAnnotation;

public class QuarkusSchedulerASTVisitor extends JavaASTValidator {

	private static Logger LOGGER = Logger.getLogger(QuarkusSchedulerASTVisitor.class.getName());

	public QuarkusSchedulerASTVisitor() {
		super();
	}

	@Override
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, QuarkusConstants.SCHEDULED_ANNOTATION) != null;
	}

	@Override
	public void visitMethod(PsiMethod node) {
		for (PsiAnnotation annotation : node.getAnnotations()) {
			if (isMatchAnnotation(annotation, QuarkusConstants.SCHEDULED_ANNOTATION)) {
				try {
					validateScheduledAnnotation(node, annotation);
				} catch (ClassCastException e) {
					;
				}
			}
		}
	}

	/**
	 * Checks if the given method declaration has a @Scheduled annotation, and if
	 * so, provides diagnostics it's member(s)
	 *
	 * @param node       The method declaration to validate
	 * @param annotation The @Scheduled annotation
	 */
	private void validateScheduledAnnotation(PsiMethod node, PsiAnnotation annotation) {
		PsiAnnotationMemberValue cronExpr = getAnnotationMemberValueExpression(annotation,
				QuarkusConstants.SCHEDULED_ANNOTATION_CRON);
		if (cronExpr instanceof PsiLiteral cronExprLit && cronExprLit.getValue() instanceof String cronValue) {
			if (!checkedEnvDiagnostic(cronExpr, cronValue, SchedulerUtils.ValidationType.cron)) {
				SchedulerErrorCodes cronPartFault = SchedulerUtils.validateCronPattern(cronValue);
				if (cronPartFault != null) {
					super.addDiagnostic(cronPartFault.getErrorMessage(), QuarkusConstants.QUARKUS_PREFIX, cronExpr,
							cronPartFault, DiagnosticSeverity.Warning);
				}
			}
		}
		PsiAnnotationMemberValue everyExpr = getAnnotationMemberValueExpression(annotation,
				QuarkusConstants.SCHEDULED_ANNOTATION_EVERY);
		if (everyExpr instanceof PsiLiteral everyExprLit && everyExprLit.getValue() instanceof String) {
			durationParseDiagnostics(everyExpr);
		}
		PsiAnnotationMemberValue delayedExpr = getAnnotationMemberValueExpression(annotation,
				QuarkusConstants.SCHEDULED_ANNOTATION_DELAYED);
		if (delayedExpr instanceof PsiLiteral delayedExprLit && delayedExprLit.getValue() instanceof String) {
			durationParseDiagnostics(delayedExpr);
		}
	}

	/**
	 * Add diagnostics for members that rely on Duration parser, i.e. every, delayed
	 *
	 * @param expr The expression retrieved from the annotation
	 */
	private void durationParseDiagnostics(PsiAnnotationMemberValue expr) {
		String value = (String) ((PsiLiteral) expr).getValue();
		if (!checkedEnvDiagnostic(expr, value, SchedulerUtils.ValidationType.duration)) {
			SchedulerErrorCodes memberFault = SchedulerUtils.validateDurationParse(value);
			if (memberFault != null) {
				super.addDiagnostic(memberFault.getErrorMessage(), QuarkusConstants.QUARKUS_PREFIX, expr, memberFault,
						DiagnosticSeverity.Warning);
			}
		}
	}

	/**
	 * Retrieve the SchedulerErrorCodes for env member value check
	 *
	 * @param expr           The expression retrieved from the annotation
	 * @param memberValue    The member value from expression
	 * @param validationType
	 */
	private boolean checkedEnvDiagnostic(PsiAnnotationMemberValue expr, String memberValue, SchedulerUtils.ValidationType validationType) {
		SchedulerErrorCodes malformedEnvFault = SchedulerUtils.matchEnvMember(memberValue, validationType);
		if (malformedEnvFault != null) {
			if (!SchedulerErrorCodes.VALID_EXPRESSION.equals(malformedEnvFault)) {
				super.addDiagnostic(malformedEnvFault.getErrorMessage(), QuarkusConstants.QUARKUS_PREFIX, expr,
						malformedEnvFault, DiagnosticSeverity.Warning);
			}
			return true;
		}
		return false;
	}

}