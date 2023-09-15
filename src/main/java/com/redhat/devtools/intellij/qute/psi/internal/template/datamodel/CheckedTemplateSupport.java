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

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.*;
import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.getTemplatePath;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.*;
import com.intellij.psi.util.ClassUtil;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ITypeResolver;
import com.redhat.devtools.intellij.qute.psi.internal.template.TemplateDataSupport;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractAnnotationTypeReferenceDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;

import com.redhat.devtools.intellij.qute.psi.utils.TemplatePathInfo;
import com.redhat.qute.commons.datamodel.DataModelBaseTemplate;
import com.redhat.qute.commons.datamodel.DataModelFragment;
import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelTemplate;

/**
 * CheckedTemplate support for template files:
 *
 * <code>
 * &#64;CheckedTemplate
 * static class Templates {
 * static native TemplateInstance items(List<Item> items);
 * }
 * <p>
 * ...
 * <p>
 * &#64;GET
 * &#64;Produces(MediaType.TEXT_HTML)
 * public TemplateInstance get() {
 * List<Item> items = new ArrayList<>();
 * items.add(new Item(new BigDecimal(10), "Apple"));
 * items.add(new Item(new BigDecimal(16), "Pear"));
 * items.add(new Item(new BigDecimal(30), "Orange"));
 * return Templates.items(items);
 * }
 * </code>
 *
 * @author Angelo ZERR
 */
public class CheckedTemplateSupport extends AbstractAnnotationTypeReferenceDataModelProvider {

    private static final Logger LOGGER = Logger.getLogger(CheckedTemplateSupport.class.getName());

    private static final String[] ANNOTATION_NAMES = {CHECKED_TEMPLATE_ANNOTATION, OLD_CHECKED_TEMPLATE_ANNOTATION};

    @Override
    protected String[] getAnnotationNames() {
        return ANNOTATION_NAMES;
    }

    @Override
    protected void processAnnotation(PsiElement javaElement, PsiAnnotation checkedTemplateAnnotation, String annotationName,
                                     SearchContext context, ProgressIndicator monitor) {
        if (javaElement instanceof PsiClass) {
            PsiClass type = (PsiClass) javaElement;
            boolean ignoreFragments = isIgnoreFragments(checkedTemplateAnnotation);
            collectDataModelTemplateForCheckedTemplate(type, ignoreFragments, context.getTypeResolver(type),
                    context.getDataModelProject().getTemplates(), monitor);
        }
    }

    /**
     * Returns true if @CheckedTemplate annotation declares that fragment must be
     * ignored and false otherwise.
     *
     * <code>
     *
     * @param checkedTemplateAnnotation the CheckedTemplate annotation.
     * @return true if @CheckedTemplate annotation declares that fragment must be
     * ignored and false otherwise.
     * @CheckedTemplate(ignoreFragments=true) </code>
     */
    private static boolean isIgnoreFragments(PsiAnnotation checkedTemplateAnnotation) {
        Boolean ignoreFragment = null;
        try {
            for (PsiNameValuePair pair : checkedTemplateAnnotation.getParameterList().getAttributes()) {
                if (CHECKED_TEMPLATE_ANNOTATION_IGNORE_FRAGMENTS.equalsIgnoreCase(pair.getAttributeName())) {
                    ignoreFragment = AnnotationUtils.getValueAsBoolean(pair);
                }
            }
        } catch (IndexNotReadyException | ProcessCanceledException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            // Do nothing
        }
        return ignoreFragment != null ? ignoreFragment.booleanValue() : false;
    }

    /**
     * Collect data model template from @CheckedTemplate.
     *
     * @param type            the Java type.
     * @param ignoreFragments true if fragments must be ignored and false otherwise.
     * @param templates       the data model templates to update with collect of template.
     * @param monitor         the progress monitor.
     */
    private static void collectDataModelTemplateForCheckedTemplate(PsiClass type, boolean ignoreFragments, ITypeResolver typeResolver,
                                                                   List<DataModelTemplate<DataModelParameter>> templates, ProgressIndicator monitor) {
        boolean innerClass = type.getContainingClass() != null;
        String className = !innerClass ? null
                : PsiTypeUtils.getSimpleClassName(type.getContainingFile().getName());

        // Loop for each methods (book, book) and create a template data model per
        // method.
        PsiMethod[] methods = type.getMethods();
        for (PsiMethod method : methods) {
            // src/main/resources/templates/${className}/${methodName}.qute.html
            TemplatePathInfo templatePathInfo = getTemplatePath(className, method.getName(), ignoreFragments);

            // Get or create template
            String templateUri = templatePathInfo.getTemplateUri();
            String fragmentId = templatePathInfo.getFragmentId();

            DataModelTemplate<DataModelParameter> template = null;
            Optional<DataModelTemplate<DataModelParameter>> existingTemplate = templates.stream()
                    .filter(t -> t.getTemplateUri().equals(templateUri)) //
                    .findFirst();
            if (existingTemplate.isEmpty()) {
                template = createTemplateDataModel(templateUri, method, type);
                templates.add(template);
            } else {
                template = existingTemplate.get();
                if (fragmentId == null) {
                    template.setSourceMethod(method.getName());
                }
            }

            if (fragmentId != null && fragmentId.length() > 0) {
                // The method name has '$' to define fragment id (ex : foo$bar)
                // Create fragment
                DataModelFragment<DataModelParameter> fragment = createFragmentDataModel(fragmentId, method, type);
                template.addFragment(fragment);
                // collect parameters for the fragment
                collectParameters(method, typeResolver, fragment, monitor);
            } else {
                // collect parameters for the template
                collectParameters(method, typeResolver, template, monitor);
            }
        }
    }

    private static DataModelTemplate<DataModelParameter> createTemplateDataModel(String templateUri, PsiMethod method, PsiClass type) {
        String methodName = method.getName();

        // Create template data model with:
        // - template uri : Qute template file which must be bind with data model.
        // - source type : the Java class which defines Templates
        // - source method: : the Java method which defines Template
        DataModelTemplate<DataModelParameter> template = new DataModelTemplate<DataModelParameter>();
        template.setParameters(new ArrayList<>());
        template.setTemplateUri(templateUri);
        template.setSourceType(ClassUtil.getJVMClassName(type));
        template.setSourceMethod(methodName);
        return template;
    }

    private static DataModelFragment<DataModelParameter> createFragmentDataModel(String fragmentId, PsiMethod method,
                                                                                 PsiClass type) {
        DataModelFragment<DataModelParameter> template = new DataModelFragment<DataModelParameter>();
        template.setParameters(new ArrayList<>());
        template.setId(fragmentId);
        template.setSourceType(type.getQualifiedName());
        template.setSourceMethod(method.getName());
        return template;
    }

    public static void collectParameters(PsiMethod method, ITypeResolver typeResolver,
                                         DataModelBaseTemplate<DataModelParameter> templateOrFragment, ProgressIndicator monitor) {
        try {
            PsiParameterList parameters = method.getParameterList();
            if (!parameters.isEmpty()) {
                boolean varargs = method.isVarArgs();
                for (int i = 0; i < parameters.getParametersCount(); i++) {
                    DataModelParameter parameter = createParameterDataModel(parameters.getParameter(i),
                            varargs && i == parameters.getParametersCount() - 1, typeResolver);
                    templateOrFragment.getParameters().add(parameter);
                }
            }

        } catch (IndexNotReadyException | ProcessCanceledException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Error while getting method template parameter of '" + method.getName() + "'.", e);
        }
        // Collect data parameters for the given template
        TemplateDataSupport.collectParametersFromDataMethodInvocation(method, templateOrFragment, monitor);
    }

    private static DataModelParameter createParameterDataModel(PsiParameter methodParameter, boolean varags,
                                                               ITypeResolver typeResolver) {
        String parameterName = methodParameter.getName();
        String parameterType = typeResolver.resolveLocalVariableSignature(methodParameter, varags);

        DataModelParameter parameter = new DataModelParameter();
        parameter.setKey(parameterName);
        parameter.setSourceType(parameterType);
        return parameter;
    }
}
