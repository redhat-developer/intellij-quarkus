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
import java.util.logging.Logger;
import java.util.regex.Matcher;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJvmModifiersOwner;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.graphql.MicroProfileGraphQLConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.graphql.TypeSystemDirectiveLocation;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import org.eclipse.lsp4j.DiagnosticSeverity;

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

    private static final String NO_VOID_MESSAGE = "Methods annotated with microprofile-graphql's `@Query` cannot have 'void' as a return type.";
    private static final String NO_VOID_MUTATION_MESSAGE = "Methods annotated with microprofile-graphql's `@Mutation` cannot have 'void' as a return type.";
    private static final String WRONG_DIRECTIVE_PLACEMENT = "Directive ''{0}'' is not allowed on element type ''{1}''";
    private static final String SUBSCRIPTION_MUST_RETURN_MULTI = "Methods annotated with `@Subscription` have to return either `io.smallrye.mutiny.Multi` or `java.util.concurrent.Flow.Publisher`.";
    private static final String SINGLE_RESULT_OPERATION_MUST_NOT_RETURN_MULTI = "Methods annotated with `@Query` or `@Mutation` cannot return `io.smallrye.mutiny.Multi` or `java.util.concurrent.Flow.Publisher`.";

    boolean allowsVoidReturnFromOperations = true;


    @Override
    public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
        Module javaProject = context.getJavaProject();
        if(PsiTypeUtils.findType(javaProject, MicroProfileGraphQLConstants.QUERY_ANNOTATION) == null) {
            return false;
        }
        // void GraphQL operations are allowed in Quarkus 3.1 and higher
        // if we're on an unknown version, allow them too
        allowsVoidReturnFromOperations = QuarkusModuleUtil.checkQuarkusVersion(context.getJavaProject(),
                matcher -> !matcher.matches() || (Integer.parseInt(matcher.group(1)) > 3 ||
                        (Integer.parseInt(matcher.group(1)) == 3) && Integer.parseInt(matcher.group(2)) > 0),
                true);
        LOGGER.fine("Allowing void return from GraphQL operations? " + allowsVoidReturnFromOperations);
        return true;
    }

    @Override
    public void visitMethod(PsiMethod node) {
        validateDirectivesOnMethod(node);
        if(!allowsVoidReturnFromOperations) {
            validateNoVoidReturnedFromOperations(node);
        }
        validateMultiReturnTypeFromSubscriptions(node);
        validateNoMultiReturnTypeFromQueriesAndMutations(node);
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
        else if (node.isEnum()) {
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
                LOGGER.fine("Checking directive: " + annotation.getQualifiedName() + " on node: " + node + " (location type = " + actualLocation.name() + ")");
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

    private void validateNoVoidReturnedFromOperations(PsiMethod node) {
        // ignore constructors, and non-void methods for now, it's faster than iterating through all annotations
        if (node.getReturnTypeElement() == null ||
                !PsiType.VOID.equals(node.getReturnType())) {
            return;
        }
        for (PsiAnnotation annotation : node.getAnnotations()) {
            if (isMatchAnnotation(annotation, MicroProfileGraphQLConstants.QUERY_ANNOTATION) ) {
                super.addDiagnostic(NO_VOID_MESSAGE, //
                        MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE, //
                        node.getReturnTypeElement(), //
                        MicroProfileGraphQLErrorCode.NO_VOID_QUERIES, //
                        DiagnosticSeverity.Error);
            } else if (isMatchAnnotation(annotation, MicroProfileGraphQLConstants.MUTATION_ANNOTATION)) {
                super.addDiagnostic(NO_VOID_MUTATION_MESSAGE, //
                        MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE, //
                        node.getReturnTypeElement(), //
                        MicroProfileGraphQLErrorCode.NO_VOID_MUTATIONS, //
                        DiagnosticSeverity.Error);
            }
        }
    }

    /**
     * A method annotated with `@Subscription` must return a `Multi` or `Flow.Publisher`.
     */
    private void validateMultiReturnTypeFromSubscriptions(PsiMethod node) {
        if(node.getReturnType() == null) {
            return;
        }
        for (PsiAnnotation annotation : node.getAnnotations()) {
            if (isMatchAnnotation(annotation, MicroProfileGraphQLConstants.SUBSCRIPTION_ANNOTATION)) {
                if(node.getReturnType().equals(PsiType.VOID)) {
                    super.addDiagnostic(SUBSCRIPTION_MUST_RETURN_MULTI,
                            MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE,
                            node.getReturnTypeElement(),
                            MicroProfileGraphQLErrorCode.SUBSCRIPTION_MUST_RETURN_MULTI,
                            DiagnosticSeverity.Error);
                    return;
                }
                if (node.getReturnType() instanceof PsiClassReferenceType) {
                    String returnTypeName = ((PsiClassReferenceType) node.getReturnType()).getReference().getQualifiedName();
                    if (!returnTypeName.equals(MicroProfileGraphQLConstants.MULTI)
                            && !returnTypeName.equals(MicroProfileGraphQLConstants.FLOW_PUBLISHER)) {
                        super.addDiagnostic(SUBSCRIPTION_MUST_RETURN_MULTI,
                                MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE,
                                node.getReturnTypeElement(),
                                MicroProfileGraphQLErrorCode.SUBSCRIPTION_MUST_RETURN_MULTI,
                                DiagnosticSeverity.Error);
                    }

                }
            }
        }
    }

    /**
     * Methods annotated with `@Query` or `@Mutation` must NOT return
     * a `Multi` or `Flow.Publisher` type.
     */
    private void validateNoMultiReturnTypeFromQueriesAndMutations(PsiMethod node) {
        if(node.getReturnType() == null) {
            return;
        }
        for (PsiAnnotation annotation : node.getAnnotations()) {
            if (isMatchAnnotation(annotation, MicroProfileGraphQLConstants.QUERY_ANNOTATION)
                    || isMatchAnnotation(annotation, MicroProfileGraphQLConstants.MUTATION_ANNOTATION)) {
                if (node.getReturnType() instanceof PsiClassReferenceType) {
                    String returnTypeName = ((PsiClassReferenceType) node.getReturnType()).getReference().getQualifiedName();
                    if (returnTypeName.equals(MicroProfileGraphQLConstants.MULTI)
                            || returnTypeName.equals(MicroProfileGraphQLConstants.FLOW_PUBLISHER)) {
                        super.addDiagnostic(SINGLE_RESULT_OPERATION_MUST_NOT_RETURN_MULTI,
                                MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE,
                                node.getReturnTypeElement(),
                                MicroProfileGraphQLErrorCode.SINGLE_RESULT_OPERATION_MUST_NOT_RETURN_MULTI,
                                DiagnosticSeverity.Error);
                    }
                }
            }
        }
    }

}