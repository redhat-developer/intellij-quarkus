/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
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


import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.*;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ITypeResolver;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractAnnotationTypeReferenceDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;
import org.apache.commons.lang3.StringUtils;

import com.redhat.qute.commons.datamodel.resolvers.MessageResolverData;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverKind;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.*;

/**
 * Type-safe Message Bundles support.
 *
 * @author Angelo ZERR
 * @see <a href="https://quarkus.io/guides/qute-reference#type-safe-message-bundles">https://quarkus.io/guides/qute-reference#type-safe-message-bundles</a>
 */
public class TypeSafeMessageBundlesSupport extends AbstractAnnotationTypeReferenceDataModelProvider {

    private static final String DEFAULT_MESSAGE_NAMESPACE = "msg";

    private static final Logger LOGGER = Logger.getLogger(TemplateGlobalAnnotationSupport.class.getName());

    private static final String[] ANNOTATION_NAMES = {MESSAGE_ANNOTATION};

    @Override
    protected String[] getAnnotationNames() {
        return ANNOTATION_NAMES;
    }

    @Override
    protected void processAnnotation(PsiElement javaElement, PsiAnnotation annotation, String annotationName, SearchContext context, ProgressIndicator monitor) {
        if (!(javaElement instanceof PsiMember)) {
            return;
        }
        if (annotation == null) {
            return;
        }
        ITypeResolver typeResolver = QuteSupportForTemplate.createTypeResolver((PsiMember) javaElement, context.getJavaProject());
        if (javaElement instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) javaElement;
            collectResolversForMessage(method, annotation, context.getDataModelProject().getValueResolvers(),
                    typeResolver);
        }
    }

    private void collectResolversForMessage(PsiMethod method, PsiAnnotation messageAnnotation,
                                            List<ValueResolverInfo> resolvers, ITypeResolver typeResolver) {

        // @MessageBundle
        // public interface AppMessages {
        PsiAnnotation messageBundleAnnotation = getMessageBundleAnnotation(method.getContainingClass());
        String sourceType = method.getContainingClass().getQualifiedName();
        ValueResolverInfo resolver = new ValueResolverInfo();
        String namespace = getNamespaceMessage(messageBundleAnnotation);
        resolver.setNamespace(namespace);
        resolver.setSourceType(sourceType);
        resolver.setSignature(typeResolver.resolveSignature(method));
        resolver.setKind(ValueResolverKind.Message);

        // data message
        String locale = getLocaleMessage(messageBundleAnnotation);
        String messageContent = getMessageContent(messageAnnotation);
        if (locale != null || messageContent != null) {
            MessageResolverData data = new MessageResolverData();
            data.setLocale(locale);
            data.setMessage(messageContent);
            resolver.setData(data);
        }

        if (!resolvers.contains(resolver)) {
            resolvers.add(resolver);
        }

    }

    private static PsiAnnotation getMessageBundleAnnotation(PsiClass type) {
        try {
            return AnnotationUtils.getAnnotation(type, MESSAGE_BUNDLE_ANNOTATION);
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while getting @MessageBundle annotation value.", e);
            return null;
        }
    }

    private static String getNamespaceMessage(PsiAnnotation messageBundleAnnotation) {
        String namespace = null;
        try {
            if (messageBundleAnnotation != null) {
                namespace = AnnotationUtils.getAnnotationMemberValue(messageBundleAnnotation, VALUE_ANNOTATION_NAME);
            }
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while getting @MessageBundle#value annotation value.", e);
            return null;
        }
        return StringUtils.isEmpty(namespace) ? DEFAULT_MESSAGE_NAMESPACE : namespace;
    }

    private static String getLocaleMessage(PsiAnnotation messageBundleAnnotation) {
        try {
            if (messageBundleAnnotation != null) {
                return AnnotationUtils.getAnnotationMemberValue(messageBundleAnnotation,
                        MESSAGE_BUNDLE_ANNOTATION_LOCALE);
            }
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while getting @MessageBundle#locale annotation value.", e);
            return null;
        }
        return null;
    }

    private static String getMessageContent(PsiAnnotation messageAnnotation) {
        try {
            return AnnotationUtils.getAnnotationMemberValue(messageAnnotation, VALUE_ANNOTATION_NAME);
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while getting @Message#value annotation value.", e);
            return null;
        }
    }
}
