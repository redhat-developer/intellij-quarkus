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
package com.redhat.devtools.intellij.qute.psi.internal.template.datamodel;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.CHECKED_TEMPLATE_ANNOTATION;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.OLD_CHECKED_TEMPLATE_ANNOTATION;
import static com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils.resolveSignature;
import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.getTemplatePath;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.ClassUtil;
import com.redhat.devtools.intellij.qute.psi.internal.template.TemplateDataSupport;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractAnnotationTypeReferenceDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;

import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelTemplate;

/**
 * CheckedTemplate support for template files:
 * 
 * <code>
 * &#64;CheckedTemplate
 *	static class Templates {
 *		static native TemplateInstance items(List<Item> items);
 *	}
 * 
 *  ...
 *  
 *  &#64;GET
 *	&#64;Produces(MediaType.TEXT_HTML)
 *	public TemplateInstance get() {
 * 		List<Item> items = new ArrayList<>();
 * 		items.add(new Item(new BigDecimal(10), "Apple"));
 * 		items.add(new Item(new BigDecimal(16), "Pear"));
 * 		items.add(new Item(new BigDecimal(30), "Orange"));
 * 		return Templates.items(items);
 *	}
 * </code>
 * 
 * 
 * @author Angelo ZERR
 *
 */
public class CheckedTemplateSupport extends AbstractAnnotationTypeReferenceDataModelProvider {

	private static final Logger LOGGER = Logger.getLogger(CheckedTemplateSupport.class.getName());

	private static final String[] ANNOTATION_NAMES = { CHECKED_TEMPLATE_ANNOTATION, OLD_CHECKED_TEMPLATE_ANNOTATION };

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(PsiElement javaElement, PsiAnnotation annotation, String annotationName,
									 SearchContext context, ProgressIndicator monitor) {
		if (javaElement instanceof PsiClass) {
			PsiClass type = (PsiClass) javaElement;
			collectDataModelTemplateForCheckedTemplate(type, context.getDataModelProject().getTemplates(), monitor);
		}
	}

	/**
	 * Collect data model template from @CheckedTemplate.
	 * 
	 * @param type      the Java type.
	 * @param templates the data model templates to update with collect of template.
	 * @param monitor   the progress monitor.
	 */
	private static void collectDataModelTemplateForCheckedTemplate(PsiClass type,
			List<DataModelTemplate<DataModelParameter>> templates, ProgressIndicator monitor) {
		boolean innerClass = type.getContainingClass() != null;
		String className = !innerClass ? null
				: PsiTypeUtils.getSimpleClassName(type.getContainingFile().getName());

		// Loop for each methods (book, book) and create a template data model per
		// method.
		PsiMethod[] methods = type.getMethods();
		for (PsiMethod method : methods) {
			DataModelTemplate<DataModelParameter> template = createTemplateDataModel(method, className, type, monitor);
			templates.add(template);
		}
	}

	private static DataModelTemplate<DataModelParameter> createTemplateDataModel(PsiMethod method, String className,
			PsiClass type, ProgressIndicator monitor) {
		String methodName = method.getName();
		// src/main/resources/templates/${className}/${methodName}.qute.html
		String templateUri = getTemplatePath(className, methodName);

		// Create template data model with:
		// - template uri : Qute template file which must be bind with data model.
		// - source type : the Java class which defines Templates
		// -
		DataModelTemplate<DataModelParameter> template = new DataModelTemplate<DataModelParameter>();
		template.setParameters(new ArrayList<>());
		template.setTemplateUri(templateUri);
		template.setSourceType(ClassUtil.getJVMClassName(type));
		template.setSourceMethod(methodName);

		try {
			for (PsiParameter methodParameter : method.getParameterList().getParameters()) {
				DataModelParameter parameter = createParameterDataModel(methodParameter, type);
				template.getParameters().add(parameter);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,
					"Error while getting method template parameter of '" + method.getName() + "'.", e);
		}
		// Collect data parameters for the given template
		TemplateDataSupport.collectParametersFromDataMethodInvocation(method, template, monitor);
		return template;
	}

	private static DataModelParameter createParameterDataModel(PsiParameter methodParameter, PsiClass type) {
		String parameterName = methodParameter.getName();
		String parameterType = resolveSignature(methodParameter, type);

		DataModelParameter parameter = new DataModelParameter();
		parameter.setKey(parameterName);
		parameter.setSourceType(parameterType);
		return parameter;
	}

}
