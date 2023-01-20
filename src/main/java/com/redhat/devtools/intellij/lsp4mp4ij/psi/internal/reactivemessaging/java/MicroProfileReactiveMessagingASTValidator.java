/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.reactivemessaging.java;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiLiteral;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.reactivemessaging.MicroProfileReactiveMessagingConstants.INCOMING_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.reactivemessaging.MicroProfileReactiveMessagingConstants.MICRO_PROFILE_REACTIVE_MESSAGING_DIAGNOSTIC_SOURCE;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.reactivemessaging.MicroProfileReactiveMessagingConstants.OUTGOING_ANNOTATION;


/**
 *
 * MicroProfile Reactive Messaging Diagnostics:
 *
 * <ul>
 * <li>Diagnostic: display reactive messaging diagnostic message if the name of
 * the consumed channel is blank.</li>
 * </ul>
 *
 * <p>
 * This rule comes from
 * https://github.com/eclipse/microprofile-reactive-messaging/blob/62c9ed5dffe01125941bb185f1433d6307b83c86/api/src/main/java/org/eclipse/microprofile/reactive/messaging/Incoming.java#L95
 * </p>
 * 
 * @See https://github.com/eclipse/microprofile-reactive-messaging
 *
 */
public class MicroProfileReactiveMessagingASTValidator extends JavaASTValidator {

    private static final String BLANK_CHANNEL_NAME_MESSAGE = "The name of the consumed channel must not be blank.";

    private static final String ATTRIBUTE_VALUE = "value";

    private static final Logger LOGGER = Logger.getLogger(MicroProfileReactiveMessagingASTValidator.class.getName());

    @Override
    public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
        Module javaProject = context.getJavaProject();
        boolean adapted = PsiTypeUtils.findType(javaProject, INCOMING_ANNOTATION) != null
                || PsiTypeUtils.findType(javaProject, OUTGOING_ANNOTATION) != null;
        return adapted;
    }

    @Override
    public void visitAnnotation(PsiAnnotation node) {
        validateIncomingOutgoingAnnotation(node);
    }

    private void validateIncomingOutgoingAnnotation(PsiAnnotation node) {
        try {
            PsiAnnotationMemberValue expression = AnnotationUtils.getAnnotationMemberValueExpression(node, ATTRIBUTE_VALUE);
            if (expression instanceof PsiLiteral
                    && ((PsiLiteral) expression).getValue() instanceof String && ((String) ((PsiLiteral) expression).getValue()).isBlank()) {
                super.addDiagnostic(BLANK_CHANNEL_NAME_MESSAGE, MICRO_PROFILE_REACTIVE_MESSAGING_DIAGNOSTIC_SOURCE,
                        expression, MicroProfileReactiveMessagingErrorCode.BLANK_CHANNEL_NAME,
                        DiagnosticSeverity.Error);
            }
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Exception when trying to validate @Incoming/@Outgoing annotation", e);
        }
    }

}
