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

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.LOCATION_ANNOTATION;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_CLASS;
import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.getTemplatePath;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiField;
import com.redhat.devtools.intellij.qute.psi.internal.template.TemplateDataSupport;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractFieldDeclarationTypeReferenceDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;

import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelTemplate;

/**
 * Template field support.
 * 
 * <code>
 *  &#64;Inject
 *  Template hello;
 *
 *  ...
 *
 *   &#64;GET
 *   &#64;Produces(MediaType.TEXT_HTML)
 *   public TemplateInstance get(@QueryParam("name") String name) {
 *   	hello.data("age", 12);
 *   	hello.data("height", 1.50, "weight", 50L);
 *       return hello.data("name", name);
 *   }
 *   </code>
 * 
 * @author Angelo ZERR
 *
 */
public class TemplateFieldSupport extends AbstractFieldDeclarationTypeReferenceDataModelProvider {

	private static final Logger LOGGER = Logger.getLogger(TemplateFieldSupport.class.getName());

	private static final String[] TYPE_NAMES = { TEMPLATE_CLASS };

	@Override
	protected String[] getTypeNames() {
		// Pattern to retrieve Template field
		return TYPE_NAMES;
	}

	@Override
	protected void processField(PsiField field, SearchContext context, ProgressIndicator monitor) {
		collectDataModelTemplateForTemplateField(field, context.getDataModelProject().getTemplates(), monitor);
	}

	private static void collectDataModelTemplateForTemplateField(PsiField field,
			List<DataModelTemplate<DataModelParameter>> templates, ProgressIndicator monitor) {
		DataModelTemplate<DataModelParameter> template = createTemplateDataModel(field, monitor);
		templates.add(template);
	}

	private static DataModelTemplate<DataModelParameter> createTemplateDataModel(PsiField field,
			ProgressIndicator monitor) {

		String location = getLocation(field);
		String fieldName = field.getName();
		// src/main/resources/templates/${methodName}.qute.html
		String templateUri = getTemplatePath(null, location != null ? location : fieldName);

		// Create template data model with:
		// - template uri : Qute template file which must be bind with data model.
		// - source type : the Java class which defines Templates
		// -
		DataModelTemplate<DataModelParameter> template = new DataModelTemplate<DataModelParameter>();
		template.setParameters(new ArrayList<>());
		template.setTemplateUri(templateUri);
		template.setSourceType(field.getContainingClass().getQualifiedName());
		template.setSourceField(fieldName);
		// Collect data parameters for the given template
		TemplateDataSupport.collectParametersFromDataMethodInvocation(field, template, monitor);
		return template;
	}

	private static String getLocation(PsiField field) {
		try {
			PsiAnnotation annotation = AnnotationUtils.getAnnotation(field, LOCATION_ANNOTATION);
			if (annotation != null) {
				return AnnotationUtils.getAnnotationMemberValue(annotation, "value");
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while getting @Location of '" + field.getName() + "'.", e);
		}
		return null;
	}
}
