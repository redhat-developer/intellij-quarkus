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

import com.google.gson.JsonObject;
import com.intellij.openapi.module.Module;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.properties.MicroProfileConfigPropertyProvider;
import com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.utils.AntPathMatcher;

import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTIES_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_NAME;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.DIAGNOSTIC_DATA_NAME;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValueExpression;

/**
 * Collects diagnostics related to the <code>@ConfigProperty</code> annotation
 * in a Java file.
 *
 * Produces diagnostics when:
 * <ul>
 * <li>The <code>defaultValue</code> attribute value cannot be represented by
 * the type of the field being annotated</li>
 * <li>The config property defined by the annotation doesn't have a default
 * value and doesn't have a value assigned to it in any properties file</li>
 * </ul>
 *
 */
public class MicroProfileConfigASTValidator extends JavaASTValidator {

	private static final Logger LOGGER = Logger.getLogger(MicroProfileConfigASTValidator.class.getName());

	private static final AntPathMatcher pathMatcher = new AntPathMatcher();

	private static final Pattern ARRAY_SPLITTER = Pattern.compile("(?<!\\\\),");

	private static final String EXPECTED_TYPE_ERROR_MESSAGE = "''{0}'' does not match the expected type of ''{1}''.";

	private static final String EMPTY_LIST_LIKE_WARNING_MESSAGE = "''defaultValue=\"\"'' will behave as if no default value is set, and will not be treated as an empty ''{0}''.";

	private static final String NO_VALUE_ERROR_MESSAGE = "The property ''{0}'' is not assigned a value in any config file, and must be assigned at runtime.";

	private static final String EMPTY_KEY_ERROR_MESSAGE = "The member ''{0}'' can'''t be empty.";

	private List<String> patterns;
	// prefix from @ConfigProperties(prefix="")
	private String currentPrefix;

	@Override
	public void initialize(JavaDiagnosticsContext context, List<Diagnostic> diagnostics) {
		super.initialize(context, diagnostics);
		this.currentPrefix = null;
		this.patterns = getPatternsFromContext(context);
	}

	@Override
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, CONFIG_PROPERTY_ANNOTATION) != null;
	}

	private static List<String> getPatternsFromContext(JavaDiagnosticsContext context) {
		return context.getSettings().getPatterns();
	}

	@Override
	public void visitClass(PsiClass typeDeclaration) {
		// Get prefix from @ConfigProperties(prefix="")
		for(PsiAnnotation annotation : typeDeclaration.getAnnotations()) {
			if (AnnotationUtils.isMatchAnnotation(annotation, CONFIG_PROPERTIES_ANNOTATION)) {
				PsiAnnotationMemberValue prefixExpr = getAnnotationMemberValueExpression(annotation, MicroProfileConfigConstants.CONFIG_PROPERTIES_ANNOTATION_PREFIX);
				if (prefixExpr instanceof PsiLiteral && ((PsiLiteral) prefixExpr).getValue() instanceof String) {
					currentPrefix = (String) ((PsiLiteral) prefixExpr).getValue();
				}

			} else if (AnnotationUtils.isMatchAnnotation(annotation, QuarkusConstants.CONFIG_PROPERTIES_ANNOTATION)) {
				PsiAnnotationMemberValue prefixExpr = getAnnotationMemberValueExpression(annotation, QuarkusConstants.CONFIG_PROPERTIES_ANNOTATION_PREFIX);
				if (prefixExpr instanceof PsiLiteral && ((PsiLiteral) prefixExpr).getValue() instanceof String) {
					currentPrefix = (String) ((PsiLiteral) prefixExpr).getValue();
				}

			}
		}
		typeDeclaration.acceptChildren(this);
		this.currentPrefix = null;
	}

	@Override
	public void visitAnnotation(PsiAnnotation annotation) {
		PsiField parent = PsiTreeUtil.getParentOfType(annotation, PsiField.class);
		if (AnnotationUtils.isMatchAnnotation(annotation, CONFIG_PROPERTY_ANNOTATION) && parent != null) {
			PsiAnnotationMemberValue defaultValueExpr = getAnnotationMemberValueExpression(annotation, MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE);
			validatePropertyDefaultValue(annotation, defaultValueExpr, parent);
			validatePropertyHasValue(annotation, defaultValueExpr);
		}

	}

	/**
	 * Validate "defaultValue" attribute of <code>@ConfigProperty</code> and
	 * generate diagnostics if "defaultValue" cannot be represented by the given
	 * field type.
	 *
	 * See
	 * https://github.com/eclipse/microprofile-config/blob/master/spec/src/main/asciidoc/converters.asciidoc
	 * for more details on default converters.
	 *
	 * @param annotation       the annotation to validate the defaultValue of
	 * @param defaultValueExpr the default value expression, or null if no default
	 *                         value is defined
	 */
	private void validatePropertyDefaultValue(PsiAnnotation annotation, PsiAnnotationMemberValue defaultValueExpr,
											  PsiField parent) {
		if (defaultValueExpr instanceof PsiLiteral && ((PsiLiteral) defaultValueExpr).getValue() instanceof String && parent != null) {
			String defValue = (String) ((PsiLiteral) defaultValueExpr).getValue();
			Module javaProject = getContext().getJavaProject();
			PsiType fieldBinding = parent.getType();
			if (fieldBinding != null && defValue != null) {
				if (isListLike(fieldBinding) && defValue.isEmpty()) {
					String message = MessageFormat.format(EMPTY_LIST_LIKE_WARNING_MESSAGE, fieldBinding.getPresentableText());
					super.addDiagnostic(message, MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE, defaultValueExpr,
							MicroProfileConfigErrorCode.EMPTY_LIST_NOT_SUPPORTED, DiagnosticSeverity.Warning);
				}
				if (!isAssignable(fieldBinding, javaProject, defValue)) {
					String message = MessageFormat.format(EXPECTED_TYPE_ERROR_MESSAGE, defValue, fieldBinding.getPresentableText());
					super.addDiagnostic(message, MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE, defaultValueExpr,
							MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE, DiagnosticSeverity.Error);
				}
			}
		}
	}

	/**
	 * Generates diagnostics if the property that this <code>@ConfigProperty</code>
	 * defines does not have a value assigned to it.
	 *
	 * @param annotation       the ConfigProperty annotation
	 * @param defaultValueExpr the default value expression, or null if no default
	 *                         value is defined
	 */
	private void validatePropertyHasValue(PsiAnnotation annotation, PsiAnnotationMemberValue defaultValueExpr) {
			String name = null;
			PsiAnnotationMemberValue nameExpression = getAnnotationMemberValueExpression(annotation,
					CONFIG_PROPERTY_ANNOTATION_NAME);
			boolean hasDefaultValue = defaultValueExpr != null;

			if (nameExpression instanceof PsiLiteral && ((PsiLiteral) nameExpression).getValue() instanceof String) {
				name = (String) ((PsiLiteral) nameExpression).getValue();
				name = MicroProfileConfigPropertyProvider.getPropertyName(name, currentPrefix);
			}

		if (name != null) {
			if (name.isEmpty()) {
				String message = MessageFormat.format(EMPTY_KEY_ERROR_MESSAGE, CONFIG_PROPERTY_ANNOTATION_NAME);
				Diagnostic d = super.addDiagnostic(message, MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE, nameExpression,
						MicroProfileConfigErrorCode.EMPTY_KEY, DiagnosticSeverity.Error);
			} else if (!hasDefaultValue && !doesPropertyHaveValue(name, getContext()) && !isPropertyIgnored(name)) {
				String message = MessageFormat.format(NO_VALUE_ERROR_MESSAGE, name);
				Diagnostic d = super.addDiagnostic(message, MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE, nameExpression,
						MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY, DiagnosticSeverity.Warning);
				setDataForUnassigned(name, d);
			}
		}
	}

	private boolean isPropertyIgnored(String propertyName) {
		for (String pattern : patterns) {
			if (pathMatcher.match(pattern, propertyName)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isListLike(PsiType type) {
		if (type instanceof PsiArrayType) {
			return true;
		}
		PsiType erasedType = TypeConversionUtil.erasure(type);
		String fqn = erasedType.getCanonicalText();
		return "java.util.List".equals(fqn) || "java.util.Set".equals(fqn);
	}


	private static boolean isAssignable(PsiType fieldBinding, Module javaProject, String defValue) {
		String fqn = TypeConversionUtil.erasure(fieldBinding).getCanonicalText();
		// handle list-like types.
		// MicroProfile config supports arrays, `java.util.List`, and `java.util.Set` by
		// default. See:
		// https://download.eclipse.org/microprofile/microprofile-config-2.0/microprofile-config-spec-2.0.html#_array_converters
		if (isListLike(fieldBinding)) {
			if (defValue.isEmpty()) {
				// A different error is shown in this case
				return true;
			}
			String itemsTypeFqn = "java.lang.Object";
			if (fieldBinding instanceof PsiArrayType) {
				itemsTypeFqn = TypeConversionUtil.erasure(((PsiArrayType)fieldBinding).getComponentType()).getCanonicalText();
			} else if (fieldBinding instanceof PsiClassType) {
				PsiClassType collection = (PsiClassType) fieldBinding;
				if (collection.getParameterCount() < 1) {
					return true;
				}
				itemsTypeFqn = TypeConversionUtil.erasure(collection.getParameters()[0]).getCanonicalText();
			}
			for (String listItemValue : ARRAY_SPLITTER.split(defValue, -1)) {
				listItemValue = listItemValue.replace("\\,", ",");
				if (!isAssignable(itemsTypeFqn, listItemValue, javaProject)) {
					return false;
				}
			}
			return true;
		} else {
			// Not a list-like type
			return isAssignable(fqn, defValue, javaProject);
		}
	}

	private static boolean isAssignable(String typeFqn, String value, Module javaProject) {
		try {
			switch (typeFqn) {
				case "boolean":
				case "java.lang.Boolean":
					return Boolean.valueOf(value) != null;
				case "byte":
				case "java.lang.Byte":
					return Byte.valueOf(value) != null;
				case "short":
				case "java.lang.Short":
					return Short.valueOf(value) != null;
				case "int":
				case "java.lang.Integer":
					return Integer.valueOf(value) != null;
				case "long":
				case "java.lang.Long":
					return Long.valueOf(value) != null;
				case "float":
				case "java.lang.Float":
					return Float.valueOf(value) != null;
				case "double":
				case "java.lang.Double":
					return Double.valueOf(value) != null;
				case "char":
				case "java.lang.Character":
					if (value == null || value.length() != 1) {
						return false;
					}
					return Character.valueOf(value.charAt(0)) != null;
				case "java.lang.Class":
					return  PsiTypeUtils.findType(javaProject, value) != null;
				case "java.lang.String":
					return true;
				default:
					return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean doesPropertyHaveValue(String property, JavaDiagnosticsContext context) {
		Module javaProject = context.getJavaProject();
		PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(javaProject.getProject())
				.getJDTMicroProfileProject(javaProject);
		return mpProject.hasProperty(property);
	}

	public static void setDataForUnassigned(String name, Diagnostic diagnostic) {
		JsonObject data = new JsonObject();
		data.addProperty(DIAGNOSTIC_DATA_NAME, name);
		diagnostic.setData(data);
	}
}
