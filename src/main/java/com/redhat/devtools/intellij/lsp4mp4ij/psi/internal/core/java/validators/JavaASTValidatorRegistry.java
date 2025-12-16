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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.java.validators;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidatorExtensionPointBean;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations.AnnotationAttributeRule;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations.AnnotationRule;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations.AnnotationRuleAttributeExtensionPointBean;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations.AnnotationRuleExtensionPointBean;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations.AnnotationRulesJavaASTValidator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations.AnnotationValidator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations.RangeExpressionException;
import org.eclipse.lsp4j.Diagnostic;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry to hold the Extension point
 * "org.eclipse.lsp4mp.jdt.core.javaASTValidators".
 *
 * @author Angelo ZERR
 *
 */
public class JavaASTValidatorRegistry extends AnnotationValidator {

	private static final Logger LOGGER = Logger.getLogger(JavaASTValidatorRegistry.class.getName());

	private static final JavaASTValidatorRegistry INSTANCE = new JavaASTValidatorRegistry();

	private static final String EXTENSION_ID = "javaASTValidators";

	public static JavaASTValidatorRegistry getInstance() {
		return INSTANCE;
	}

	private boolean extensionProvidersLoaded;
	private boolean registryListenerIntialized;

	private final List<JavaASTValidatorExtensionPointBean> validatorsFromClass;

	private JavaASTValidatorRegistry() {
		super();
		this.extensionProvidersLoaded = false;
		this.registryListenerIntialized = false;
		this.validatorsFromClass = new ArrayList<>();
	}

	public String getExtensionId() {
		return EXTENSION_ID;
	}

	@Override
	public String validate(String value, AnnotationAttributeRule rule) {
		loadExtensionJavaASTValidators();
		return super.validate(value, rule);
	}

	@Override
	public Collection<AnnotationRule> getRules() {
		loadExtensionJavaASTValidators();
		return super.getRules();
	}

	private synchronized void loadExtensionJavaASTValidators() {
		if (extensionProvidersLoaded)
			return;

		// Immediately set the flag, as to ensure that this method is never
		// called twice
		extensionProvidersLoaded = true;

		LOGGER.log(Level.INFO, "->- Loading ." + getExtensionId() + " extension point ->-");

		addExtensionJavaASTValidators();

		LOGGER.log(Level.INFO, "-<- Done loading ." + getExtensionId() + " extension point -<-");
	}

	private void addExtensionJavaASTValidators() {
		try {
			validatorsFromClass.addAll(JavaASTValidator.EP_NAME.getExtensionList());
			for(AnnotationRuleExtensionPointBean bean : AnnotationValidator.EP_NAME.getExtensions()) {
				registerRule(createRule(bean));

			}
		} catch (RangeExpressionException e) {
			LOGGER.log(Level.WARNING, "  Loaded while loading " + getExtensionId(), e);
		}
	}

	private AnnotationRule createRule(AnnotationRuleExtensionPointBean bean) throws RangeExpressionException {
		AnnotationRule rule = new AnnotationRule(bean.annotation, bean.source);
		for(AnnotationRuleAttributeExtensionPointBean bean1 : bean.attributes) {
			AnnotationAttributeRule attributeRule = new AnnotationAttributeRule(bean1.name);
			attributeRule.setRange(bean1.range);
			rule.addRule(attributeRule);
		}
		return rule;
	}

	public Collection<JavaRecursiveElementVisitor> getValidators(JavaDiagnosticsContext context) {
		List<JavaRecursiveElementVisitor> validators = new ArrayList<>();
		addValidator(new AnnotationRulesJavaASTValidator(getRules()), context, validators);
		for (JavaASTValidatorExtensionPointBean ce : validatorsFromClass) {
			try {
				addValidator(ce.createValidator(), context, validators);
			} catch (ClassNotFoundException | NoSuchMethodException |
					 InvocationTargetException | InstantiationException | IllegalAccessException e) {
				LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
			}
		}
		return validators;
	}

	private void addValidator(JavaASTValidator validator, JavaDiagnosticsContext context,
			List<JavaRecursiveElementVisitor> validators) {
		validator.initialize(context);
		if (validator.isAdaptedForDiagnostics(context)) {
			validators.add(validator);
		}
	}

}