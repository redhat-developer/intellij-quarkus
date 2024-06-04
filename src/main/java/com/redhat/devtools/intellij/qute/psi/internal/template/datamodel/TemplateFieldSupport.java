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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.util.ClassUtil;
import com.redhat.devtools.intellij.qute.psi.internal.AnnotationLocationSupport;
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
 * &#64;Inject
 * Template hello;
 * <p>
 * ...
 * <p>
 * &#64;GET
 * &#64;Produces(MediaType.TEXT_HTML)
 * public TemplateInstance get(@QueryParam("name") String name) {
 * hello.data("age", 12);
 * hello.data("height", 1.50, "weight", 50L);
 * return hello.data("name", name);
 * }
 * </code>
 *
 * @author Angelo ZERR
 */
public class TemplateFieldSupport extends AbstractFieldDeclarationTypeReferenceDataModelProvider {

    private static final Logger LOGGER = Logger.getLogger(TemplateFieldSupport.class.getName());

    private static final String[] TYPE_NAMES = {TEMPLATE_CLASS};

    private static final String KEY = TemplateFieldSupport.class.getName() + "#";

    @Override
    protected String[] getTypeNames() {
        // Pattern to retrieve Template field
        return TYPE_NAMES;
    }

    @Override
    protected void processField(PsiField field, SearchContext context, ProgressIndicator monitor) {
        PsiFile compilationUnit = field.getContainingFile();
        AnnotationLocationSupport annotationLocationSupport = getAnnotationLocationSupport(compilationUnit, context);
        PsiLiteralValue location = annotationLocationSupport
                .getLocationExpressionFromConstructorParameter(field.getName());
        collectDataModelTemplateForTemplateField(field, context.getDataModelProject().getTemplates(),
                context.getRelativeTemplateBaseDir(),
                location != null ? (String) location.getValue() : null, monitor);
    }

    private static AnnotationLocationSupport getAnnotationLocationSupport(PsiFile compilationUnit,
                                                                          SearchContext context) {
        @SuppressWarnings("unchecked")
        Map<PsiFile, AnnotationLocationSupport> allSupport = (Map<PsiFile, AnnotationLocationSupport>) context
                .get(KEY);
        if (allSupport == null) {
            allSupport = new HashMap<>();
            context.put(KEY, allSupport);
        }

        AnnotationLocationSupport unitSupport = allSupport.get(compilationUnit);
        if (unitSupport == null) {
            @SuppressWarnings("restriction")
            PsiFile root = compilationUnit;
            unitSupport = new AnnotationLocationSupport(root);
            allSupport.put(compilationUnit, unitSupport);
        }
        return unitSupport;
    }

    private static void collectDataModelTemplateForTemplateField(PsiField field,
                                                                 List<DataModelTemplate<DataModelParameter>> templates, String relativeTemplateBaseDir, String location, ProgressIndicator monitor) {
        DataModelTemplate<DataModelParameter> template = createTemplateDataModel(field, relativeTemplateBaseDir, location, monitor);
        templates.add(template);
    }

    private static DataModelTemplate<DataModelParameter> createTemplateDataModel(PsiField field, String relativeTemplateBaseDir, String locationFromConstructorParameter,
                                                                                 ProgressIndicator monitor) {

        String location = locationFromConstructorParameter != null ? locationFromConstructorParameter : getLocation(field);
        String fieldName = field.getName();
        // src/main/resources/templates/${methodName}.qute.html
        String templateUri = getTemplatePath(relativeTemplateBaseDir, null, null, location != null ? location : fieldName, true).getTemplateUri();

        // Create template data model with:
        // - template uri : Qute template file which must be bind with data model.
        // - source type : the Java class which defines Templates
        // -
        DataModelTemplate<DataModelParameter> template = new DataModelTemplate<DataModelParameter>();
        template.setParameters(new ArrayList<>());
        template.setTemplateUri(templateUri);
        template.setSourceType(ClassUtil.getJVMClassName(field.getContainingClass()));
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
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while getting @Location of '" + field.getName() + "'.", e);
        }
        return null;
    }
}
