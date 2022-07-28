/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiThisExpression;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.LOCATION_ANNOTATION;

/**
 * The @Location support gives the capability to retrieve the @Location declared
 * in a parameter of constructor which assigns a Java field .
 * 
 * @author Angelo ZERR
 *
 */
public class AnnotationLocationSupport extends JavaRecursiveElementVisitor {

	// the Map of assigned fields initialized by the parameters constructor.
	private final Map<String /* the field name */, PsiParameter /*
																				 * the parameter constructor annotated
																				 * with @Location
																				 */> assignedFields;

	// Constructor parameters which have an @Location annotation
	// ex: public SomePage(@Location("foo/bar/page.qute.html") Template page)
	private Set<PsiParameter> constructorParametersAnnotatedWithLocation;

	public AnnotationLocationSupport(PsiFile root) {
		assignedFields = new HashMap();
		root.accept(this);
	}

	@Override
	public void visitMethod(PsiMethod node) {
		boolean visitBody = false;
		if (node.isConstructor()) {
			//@SuppressWarnings("rawtypes")
			//List parameters = node.getParameters();
			for (Object parameter : node.getParameterList().getParameters()) {
				if (parameter instanceof PsiParameter) {
					PsiParameter variable = (PsiParameter) parameter;
					if (getLocationExpression(variable, variable.getModifierList()) != null) {
						// The current constructor parameter has @Location annotation
						// SomePage(@Location("foo/bar/page.qute.html") Template page)
						if (constructorParametersAnnotatedWithLocation == null) {
							constructorParametersAnnotatedWithLocation = new HashSet<>();
						}
						constructorParametersAnnotatedWithLocation.add(variable);
						visitBody = true;
					}
				}
			}
		}
		// There is one parameter of the constructor which is annotated with @Location
		// visit the constructor body to fill the assigned fields.
		if (visitBody) {
			super.visitMethod(node);
		}
	}

	@Override
	public void visitAssignmentExpression(PsiAssignmentExpression node) {
		// Visit the body of the constructor to find the assigned fields with
		// constructor parameters.
		PsiExpression left = node.getLExpression();
		PsiExpression right = node.getRExpression();
		if (left == null || right == null) {
			return;
		}

		/*FieldAccess fieldAccess = (left instanceof FieldAccess) ? (FieldAccess) left : null;*/
		PsiIdentifier fieldAccess = getFieldAccess(left);
		if (fieldAccess == null) {
			return;
		}

		// Here we have a field access:
		// private Template page;
		// ...
		// public SomePage(...)
		// this.page = ... // here we have a field access

		if (right instanceof PsiReferenceExpression &&
				((PsiReferenceExpression) right).getReferenceNameElement() instanceof PsiIdentifier) {
			// public SomePage(Template page)
			// this.page = page;
			PsiParameter variable = getMatchedParameter((PsiIdentifier) ((PsiReferenceExpression) right).getReferenceNameElement(),
					constructorParametersAnnotatedWithLocation);
			if (variable != null) {
				assignedFields.put(fieldAccess.getText(), variable);
			}
		} else if (right instanceof PsiMethodCallExpression) {
			// public SomePage(Template page)
			// this.page = requireNonNull(page, "page is required");
			PsiMethodCallExpression methodInvocation = (PsiMethodCallExpression) right;
			PsiParameter variable = getMatchedParameter(methodInvocation,
					constructorParametersAnnotatedWithLocation);
			if (variable != null) {
				assignedFields.put(fieldAccess.getText(), variable);
			}
		}
		super.visitAssignmentExpression(node);
	}

	private PsiIdentifier getFieldAccess(PsiExpression left) {
		if (left instanceof PsiReferenceExpression) {
			PsiReferenceExpression referenceExpression = (PsiReferenceExpression) left;
			if (referenceExpression.getQualifierExpression() instanceof PsiThisExpression &&
					referenceExpression.getReferenceNameElement() instanceof PsiIdentifier &&
					referenceExpression.resolve() instanceof PsiField) {
				return (PsiIdentifier) referenceExpression.getReferenceNameElement();
			}
		}
		return null;
	}

	private static PsiParameter getMatchedParameter(PsiIdentifier simpleName,
			Set<PsiParameter> constructorParametersAnnotatedWithLocation) {
		String name = simpleName.getText();
		for (PsiParameter variable : constructorParametersAnnotatedWithLocation) {
			if (variable.getName().equals(name)) {
				return variable;
			}
		}
		return null;
	}

	private static PsiParameter getMatchedParameter(PsiMethodCallExpression methodInvocation,
			Set<PsiParameter> constructorParametersAnnotatedWithLocation) {
		for (Object arg : methodInvocation.getArgumentList().getExpressions()) {
			if (arg instanceof PsiReferenceExpression &&
					((PsiReferenceExpression) arg).getReferenceNameElement() instanceof PsiIdentifier) {
				PsiParameter variable = getMatchedParameter((PsiIdentifier) ((PsiReferenceExpression) arg).getReferenceNameElement(),
						constructorParametersAnnotatedWithLocation);
				if (variable != null) {
					return variable;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the @Location expression defined in the constructor parameter which
	 * initializes the given field name and null otherwise.
	 * 
	 * @param fieldName the Java field name.
	 * 
	 * @return the @Location expression defined in the constructor parameter which
	 *         initializes the given field name and null otherwise.
	 */
	public PsiLiteralValue getLocationExpressionFromConstructorParameter(String fieldName) {
		PsiParameter variable = assignedFields.get(fieldName);
		if (variable == null) {
			return null;
		}
		return getLocationExpression(variable, variable.getModifierList());
	}

	/**
	 * Returns the @Location expression annotated for the given AST node field.
	 * 
	 * @param node      the AST node field.
	 * 
	 * @param modifiers the AST node modifiers.
	 * 
	 * @return the @Location expression annotated for the given AST node field.
	 */
	public static PsiLiteralValue getLocationExpression(PsiModifierListOwner node, @SuppressWarnings("rawtypes") PsiModifierList modifiers) {
		if (modifiers == null || modifiers.getAnnotations().length == 0) {
			return null;
		}
		for (PsiAnnotation modifier : modifiers.getAnnotations()) {
			if (modifier instanceof PsiAnnotation) {
				PsiAnnotation annotation = (PsiAnnotation) modifier;
				if (AnnotationUtils.isMatchAnnotation(annotation, LOCATION_ANNOTATION)) {
					// @Location("/items/my.items.qute.html")
					// Template items;
					PsiAnnotationMemberValue expression = annotation.findDeclaredAttributeValue("value");
					if (expression != null && expression instanceof PsiLiteralValue && ((PsiLiteralValue) expression).getValue() instanceof String) {
						String location = (String) ((PsiLiteralValue) expression).getValue();
						if (StringUtils.isNotBlank(location)) {
							return (PsiLiteralValue) expression;
						}
					}
				}
			}
		}
		return null;
	}
}
