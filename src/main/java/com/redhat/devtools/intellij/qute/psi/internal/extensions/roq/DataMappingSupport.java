/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.extensions.roq;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractAnnotationTypeReferenceDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverKind;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.VALUE_ANNOTATION_NAME;
import static com.redhat.devtools.intellij.qute.psi.internal.extensions.roq.RoqJavaConstants.DATA_MAPPING_ANNOTATION;

/**
 * Roq @DataMapping annotation support.
 *
 * @author Angelo ZERR
 */
public class DataMappingSupport extends AbstractAnnotationTypeReferenceDataModelProvider {

    private static final Logger LOGGER = Logger.getLogger(DataMappingSupport.class.getName());

    private static final String INJECT_NAMESPACE = "inject";

    private static final String[] ANNOTATION_NAMES = {DATA_MAPPING_ANNOTATION};

    @Override
    protected String[] getAnnotationNames() {
        return ANNOTATION_NAMES;
    }

    @Override
    protected void processAnnotation(PsiElement javaElement, PsiAnnotation annotation, String annotationName,
                                     SearchContext context, ProgressIndicator monitor) {
        if (!(javaElement instanceof PsiClass)) {
            return;
        }
        // @DataMapping(value = "events", parentArray = true)
        // public record Events(List<Event> list) {
        // becomes --> inject:events

        PsiClass type = (PsiClass) javaElement;
        String value = getDataMappingAnnotationValue(type);
        if (StringUtils.isNoneBlank(value)) {
            collectResolversForInject(type, value, context.getDataModelProject().getValueResolvers());
        }
    }

    @Nullable
    private static String getDataMappingAnnotationValue(PsiClass javaElement) {
        PsiAnnotation namedAnnotation = AnnotationUtils.getAnnotation(javaElement,
                DATA_MAPPING_ANNOTATION);
        if (namedAnnotation != null) {
            return AnnotationUtils.getAnnotationMemberValue(namedAnnotation, VALUE_ANNOTATION_NAME);
        }
        return null;
    }

    private static void collectResolversForInject(PsiClass type, String named, List<ValueResolverInfo> resolvers) {
        ValueResolverInfo resolver = new ValueResolverInfo();
        resolver.setNamed(named);
        resolver.setSourceType(type.getQualifiedName());
        resolver.setSignature(type.getQualifiedName());
        resolver.setNamespace(INJECT_NAMESPACE);
        resolver.setKind(ValueResolverKind.InjectedBean);
        resolvers.add(resolver);
    }
}
