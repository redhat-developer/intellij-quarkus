/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.utils;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.AbstractTypeResolver;
import com.redhat.qute.commons.JavaTypeKind;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * JDT Type utilities.
 *
 * @author Angelo ZERR
 */
public class PsiTypeUtils {

    private static final Logger LOGGER = Logger.getLogger(PsiTypeUtils.class.getName());

    public static String getSimpleClassName(String className) {
        if (className.endsWith(".java")) {
            return className.substring(0, className.length() - ".java".length());
        }
        if (className.endsWith(".class")) {
            return className.substring(0, className.length() - ".class".length());
        }
        return className;
    }

    public static PsiClass findType(Module module, String name) {
        return ClassUtil.findPsiClass(PsiManager.getInstance(module.getProject()), name, null, false,
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module));
    }

    /**
     * Returns the resolved type name of the <code>javaElement</code> and null
     * otherwise
     *
     * @param javaElement the Java element
     * @return the resolved type name of the <code>javaElement</code> and null
     * otherwise
     */
    public static String getResolvedTypeName(PsiElement javaElement) {
        if (javaElement instanceof PsiVariable) {
            return getResolvedTypeName((PsiLocalVariable) javaElement);
        } else if (javaElement instanceof PsiField) {
            return getResolvedTypeName((PsiField) javaElement);
        }
        return null;
    }

    /**
     * Returns the resolved type name of the given <code>localVar</code> and null
     * otherwise
     *
     * @param localVar the local variable
     * @return the resolved type name of the given <code>localVar</code> and null
     * otherwise
     */
    public static String getResolvedTypeName(PsiLocalVariable localVar) {
        return localVar.getType().getCanonicalText();
    }

    /**
     * Returns the resolved type name of the given <code>field</code> and null
     * otherwise
     *
     * @param field the field
     * @return the resolved type name of the given <code>field</code> and null
     * otherwise
     */
    public static String getResolvedTypeName(PsiField field) {
        return field.getType().getCanonicalText();
    }

    public static String getPropertyType(PsiClass psiClass, String typeName) {
        return psiClass != null ? psiClass.getQualifiedName() : typeName;
    }

    /**
     * Returns true if the given <code>javaElement</code> is from a Java binary, and
     * false otherwise
     *
     * @param javaElement the Java element
     * @return true if the given <code>javaElement</code> is from a Java binary, and
     * false otherwise
     */
    public static boolean isBinary(PsiElement javaElement) {
        return javaElement instanceof PsiCompiledElement;
    }

    /**
     * Returns the source type of the given <code>javaElement</code> and null
     * otherwise
     *
     * @param psiElement the Java element
     * @return the source type of the <code>javaElement</code>
     */
    public static String getSourceType(PsiElement psiElement) {
        if (psiElement instanceof PsiField || psiElement instanceof PsiMethod) {
            return ClassUtil.getJVMClassName(((PsiMember) psiElement).getContainingClass());
        } else if (psiElement instanceof PsiParameter) {
            return ClassUtil.getJVMClassName(((PsiMethod) ((PsiParameter) psiElement).getDeclarationScope()).getContainingClass());
        }
        if (psiElement instanceof PsiClass) {
            return ClassUtil.getJVMClassName((PsiClass) psiElement);
        }
        return null;
    }

    /**
     * Returns the source type of the given local variable <code>member</code> and
     * null otherwise
     *
     * @param member the local variable to get the source type from
     * @return the source type of the given local variable <code>member</code> and
     * null otherwise
     */
    public static String getSourceType(PsiLocalVariable member) {
        return getSourceType(member.getParent());
    }

    public static String getSourceMethod(PsiMethod method) {
        return method.getName() + method.getSignature(PsiSubstitutor.EMPTY);
    }

    public static String resolveSignature(PsiType type, boolean varargs) {
        if (type instanceof PsiPrimitiveType) {
            // int, long, etc
            return ((PsiPrimitiveType) type).getName();
        }
        if (type instanceof PsiArrayType) {
            return resolveSignature(((PsiArrayType) type).getComponentType(), false) + (varargs ? "..." : "[]");
        }
        if (type instanceof PsiClassReferenceType) {
            var ref = ((PsiClassReferenceType) type).getReference();
            var resolvedType = ((PsiClassReferenceType) type).resolve();
            var prefix = resolvedType != null ? ClassUtil.getJVMClassName(resolvedType) : null;
            if (prefix == null) {
                prefix = ref.getReferenceName();
            }
            var types = ref.getTypeParameters();
            if (types.length > 0) {
                var suffixes = Arrays.stream(types)
                        .map(t -> resolveSignature(t, false))
                        .collect(Collectors.toList());
                prefix = prefix + '<' + String.join(",", suffixes) + '>';
            }
            return prefix;
        }
        // Returns the type without the annotation
        // ex : @jakarta.validation.constraints.NotNull int -> int
        return type.getCanonicalText(false);
    }

    public static String resolveSignature(PsiParameter methodParameter, PsiClass type, boolean varargs) {
        return resolveSignature(methodParameter.getType(), varargs);
    }

    public static String getFullQualifiedName(String name, Module javaProject, ProgressIndicator monitor) {
        if (name.indexOf('.') != -1) {
            return name;
        }
        PsiClass nameType = findType(name, javaProject, monitor);
        if (nameType != null && nameType.isValid()) {
            return AbstractTypeResolver.resolveJavaTypeSignature(nameType);
        }
        return name;
    }

    public static PsiClass findType(String className, Module javaProject, ProgressIndicator monitor) {
        try {
            PsiClass type = JavaPsiFacade.getInstance(javaProject.getProject()).findClass(className.replace('$', '.'), javaProject.getModuleWithDependenciesAndLibrariesScope(true));
            if (type != null) {
                return type;
            }
            if (className.indexOf('.') == -1) {
                // No package, try with java.lang package
                // ex : if className = String we should find type of java.lang.String
                return JavaPsiFacade.getInstance(javaProject.getProject()).findClass("java.lang." + className, javaProject.getModuleWithDependenciesAndLibrariesScope(true));
            }
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while finding type for '" + className + "'.", e);
        }
        return null;
    }


    /**
     * Return true if member is static, and false otherwise
     *
     * @param member the member to check for static
     * @return
     */
    public static boolean isStaticMember(PsiMember member) {
        return member.getModifierList().hasExplicitModifier(PsiModifier.STATIC);
    }

    /**
     * Return true if member is private, and false otherwise
     *
     * @param member the member to check for private access modifier
     * @return
     */
    public static boolean isPrivateMember(PsiMember member) {
        return member.getModifierList().hasExplicitModifier(PsiModifier.PRIVATE);
    }

    /**
     * Return true if member is public, and false otherwise
     *
     * @param member the member to check for public access modifier
     * @return
     */
    public static boolean isPublicMember(PsiMember member) {
        return member.getModifierList().hasExplicitModifier(PsiModifier.PUBLIC);
    }

    /**
     * Return true if method returns `void`, and false otherwise
     *
     * @param method the method to check return value of
     * @return
     */
    public static boolean isVoidReturnType(PsiMethod method) {
        return PsiType.VOID.equals(method.getReturnType());
    }

    /**
     * Return the JavaTypeKind of the given IType type
     *
     * @param type the IType of the type to get the JavaTypeKind of
     * @return the JavaTypeKind of the given IType type
     */
    public static JavaTypeKind getJavaTypeKind(PsiClass type) {
        if (isClass(type)) {
            return JavaTypeKind.Class;
        }
        if (type.isEnum()) {
            return JavaTypeKind.Enum;
        }
        if (type.isInterface()) {
            return JavaTypeKind.Interface;
        }
        return JavaTypeKind.Unknown;
    }

    public static boolean isClass(PsiClass type) {
        return !(type.isInterface() | type.isEnum() | type.isAnnotationType());
    }
}
