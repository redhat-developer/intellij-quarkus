/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.graphql.java;


import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Logger;

import com.intellij.lang.jvm.JvmMethod;
import com.intellij.lang.jvm.types.JvmArrayType;
import com.intellij.lang.jvm.types.JvmPrimitiveType;
import com.intellij.lang.jvm.types.JvmReferenceType;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.lang.jvm.types.JvmTypeVisitor;
import com.intellij.lang.jvm.types.JvmWildcardType;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJvmModifiersOwner;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.graphql.MicroProfileGraphQLConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.graphql.TypeSystemDirectiveLocation;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.jetbrains.annotations.NotNull;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.isMatchAnnotation;

/**
 * Diagnostics for microprofile-graphql.
 *
 * TODO: We currently don't check directives on input/output objects and their properties, because
 * it's not trivial to determine whether a class is used as an input, or an output, or both. That
 * will possibly require building the whole GraphQL schema on-the-fly, which might be too expensive.
 *
 * @see https://download.eclipse.org/microprofile/microprofile-graphql-1.0/microprofile-graphql.html
 */
public class MicroProfileGraphQLASTValidator extends JavaASTValidator {

    private static final Logger LOGGER = Logger.getLogger(MicroProfileGraphQLASTValidator.class.getName());

    private static final String WRONG_DIRECTIVE_PLACEMENT = "Directive ''{0}'' is not allowed on element type ''{1}''";

    @Override
    public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
        Module javaProject = context.getJavaProject();
        // Check if microprofile-graphql is on the path
        return PsiTypeUtils.findType(javaProject, MicroProfileGraphQLConstants.QUERY_ANNOTATION) != null;
    }

    @Override
    public void visitMethod(PsiMethod node) {
        validateDirectivesOnMethod(node);
        super.visitMethod(node);
    }

    @Override
    public void visitClass(PsiClass node) {
        validateDirectivesOnClass(node);
        super.visitClass(node);
    }


    private void validateDirectivesOnMethod(PsiMethod node) {
        for (PsiAnnotation annotation : node.getAnnotations()) {
            // a query/mutation/subscription may only have directives allowed on FIELD_DEFINITION
            if (isMatchAnnotation(annotation, MicroProfileGraphQLConstants.QUERY_ANNOTATION) ||
                    isMatchAnnotation(annotation, MicroProfileGraphQLConstants.MUTATION_ANNOTATION) ||
                    isMatchAnnotation(annotation, MicroProfileGraphQLConstants.SUBSCRIPTION_ANNOTATION)) {
                validateDirectives(node, TypeSystemDirectiveLocation.FIELD_DEFINITION);
            }
        }
        // any parameter may only have directives allowed on ARGUMENT_DEFINITION
        for (PsiParameter parameter : node.getParameterList().getParameters()) {
            validateDirectives(parameter, TypeSystemDirectiveLocation.ARGUMENT_DEFINITION);
        }
    }

    private void validateDirectivesOnClass(PsiClass node) {
        // a class with @GraphQLApi may only have directives allowed on SCHEMA
        if(getAnnotation(node, MicroProfileGraphQLConstants.GRAPHQL_API_ANNOTATION) != null) {
            validateDirectives(node, TypeSystemDirectiveLocation.SCHEMA);
        }
        // if an interface has a `@Union` annotation, it may only have directives allowed on UNION
        // otherwise it may only have directives allowed on INTERFACE
        if (node.isInterface()) {
            if(getAnnotation(node, MicroProfileGraphQLConstants.UNION_ANNOTATION) != null) {
                validateDirectives(node, TypeSystemDirectiveLocation.UNION);
            } else {
                validateDirectives(node, TypeSystemDirectiveLocation.INTERFACE);
            }
        }
        // an enum may only have directives allowed on ENUM
        if (node.isEnum()) {
            validateDirectives(node, TypeSystemDirectiveLocation.ENUM);
            // enum values may only have directives allowed on ENUM_VALUE
            for (PsiField field : node.getFields()) {
                if(field instanceof PsiEnumConstant) {
                    validateDirectives(field, TypeSystemDirectiveLocation.ENUM_VALUE);
                }
            }
        }
    }

    private void validateDirectives(PsiJvmModifiersOwner node, TypeSystemDirectiveLocation actualLocation) {
        directiveLoop:
        for (PsiAnnotation annotation : node.getAnnotations()) {
            PsiClass directiveDeclaration = getDirectiveDeclaration(annotation);
            if (directiveDeclaration != null) {
                LOGGER.severe("Checking directive: " + annotation.getQualifiedName() + " on node: " + node + " (location type = " + actualLocation.name() + ")");
                PsiArrayInitializerMemberValue allowedLocations = (PsiArrayInitializerMemberValue) directiveDeclaration
                        .getAnnotation(MicroProfileGraphQLConstants.DIRECTIVE_ANNOTATION)
                        .findAttributeValue("on");
                if (allowedLocations != null) {
                    for (PsiAnnotationMemberValue initializer : allowedLocations.getInitializers()) {
                        String allowedLocation = initializer.getText().substring(initializer.getText().indexOf(".") + 1);
                        if (allowedLocation.equals(actualLocation.name())) {
                            // ok, this directive is allowed on this element type
                            continue directiveLoop;
                        }
                    }

                    String message = MessageFormat.format(WRONG_DIRECTIVE_PLACEMENT, directiveDeclaration.getQualifiedName(), actualLocation.name());
                    super.addDiagnostic(message,
                            MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE,
                            annotation,
                            MicroProfileGraphQLErrorCode.WRONG_DIRECTIVE_PLACEMENT,
                            DiagnosticSeverity.Error);
                }
            }
        }

    }

    // If this annotation is not a directive at all, returns null.
    // If this annotation is a directive, returns its annotation class.
    // If this annotation is a container of a repeatable directive, returns the annotation class of the directive (not the container).
    private PsiClass getDirectiveDeclaration(PsiAnnotation annotation) {
        PsiClass declaration = PsiTypeUtils.findType(getContext().getJavaProject(), annotation.getQualifiedName());
        if (declaration == null) {
            return null;
        }
        if (declaration.getAnnotation(MicroProfileGraphQLConstants.DIRECTIVE_ANNOTATION) != null) {
            return declaration;
        }
        // check whether this is a container of repeatable directives
        PsiMethod[] annoParams = declaration.findMethodsByName("value", false);
        for (PsiMethod annoParam : annoParams) {
            if (annoParam.getReturnType() instanceof PsiArrayType) {
                PsiType componentType = ((PsiArrayType) annoParam.getReturnType()).getComponentType();
                if (componentType instanceof PsiClassReferenceType) {
                    PsiClass directiveDeclaration = PsiTypeUtils.findType(getContext().getJavaProject(),
                            ((PsiClassReferenceType) componentType).getReference().getQualifiedName());
                    if (directiveDeclaration != null && directiveDeclaration.getAnnotation(MicroProfileGraphQLConstants.DIRECTIVE_ANNOTATION) != null) {
                        return directiveDeclaration;
                    }
                }
            }
        }
        return null;
    }


}