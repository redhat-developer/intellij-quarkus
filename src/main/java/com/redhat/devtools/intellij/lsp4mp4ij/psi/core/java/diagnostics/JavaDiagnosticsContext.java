/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics;

import com.intellij.openapi.module.Module;
import com.intellij.psi.*;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.AbstractJavaContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.java.MicroProfileConfigErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsSettings;
import org.eclipse.lsp4mp.commons.runtime.EnumConstantsProvider;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Java diagnostics context for a given compilation unit.
 *
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/diagnostics/JavaDiagnosticsContext.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/diagnostics/JavaDiagnosticsContext.java</a>
 *
 */
public class JavaDiagnosticsContext extends AbstractJavaContext {

    private final @NotNull List<Diagnostic> diagnostics;
    private final DocumentFormat documentFormat;
    private final @NotNull MicroProfileJavaDiagnosticsSettings settings;
    private final @Nullable MicroProfileProjectRuntime projectRuntime;

    public JavaDiagnosticsContext(String uri,
                                  PsiFile typeRoot,
                                  IPsiUtils utils,
                                  @NotNull Module module,
                                  DocumentFormat documentFormat,
                                  @Nullable MicroProfileJavaDiagnosticsSettings settings,
                                  @NotNull List<Diagnostic> diagnostics) {
        super(uri, typeRoot, utils, module);
        this.diagnostics = diagnostics;
        this.documentFormat = documentFormat;
        if (settings == null) {
            this.settings = new MicroProfileJavaDiagnosticsSettings(Collections.emptyList(), DiagnosticSeverity.Error, ExecutionMode.SAFE);
        } else {
            this.settings = settings;
        }
        this.projectRuntime = getProjectRuntime();
    }

    private MicroProfileProjectRuntime getProjectRuntime() {
        try {
            PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(getJavaProject().getProject())
                    .getMicroProfileProject(getJavaProject());
            return mpProject.getProjectRuntime();
        } catch (Exception e) {
            // Do nothing
            return null;
        }
    }

    public DocumentFormat getDocumentFormat() {
        return documentFormat;
    }

    /**
     * Returns the MicroProfileJavaDiagnosticsSettings.
     * <p>
     * Should not be null.
     *
     * @return the MicroProfileJavaDiagnosticsSettings
     */
    public MicroProfileJavaDiagnosticsSettings getSettings() {
        return this.settings;
    }

    public Diagnostic addDiagnostic(String message, String source, int offset, int length, String code,
                                    DiagnosticSeverity severity) {
        PsiFile openable = getTypeRoot();
        Range range = getUtils().toRange(openable, offset, length);
        return addDiagnostic(message, range, source, code, severity);
    }

    public Diagnostic addDiagnostic(String message, Range range, String source, IJavaErrorCode code) {
        return addDiagnostic(message, range, source, code, DiagnosticSeverity.Warning);
    }

    public Diagnostic addDiagnostic(String message, Range range, String source, IJavaErrorCode code, DiagnosticSeverity severity) {
        return addDiagnostic(message, range, source, code != null ? code.getCode() : null, severity);
    }

    private Diagnostic addDiagnostic(String message, Range range, String source, String code,
                                     DiagnosticSeverity severity) {
        Diagnostic d = createDiagnostic(message, range, source, code, severity);
        diagnostics.add(d);
        return d;
    }

    private Diagnostic addDiagnostic(String message, String source, PsiElement node, String code,
                                     DiagnosticSeverity severity, int start, int end) {
        return addDiagnostic(message, source, node.getTextOffset() + start, end, code, severity);
    }

    private Diagnostic createDiagnostic(String message, Range range, String source, String code,
                                        DiagnosticSeverity severity) {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setSource(source);
        diagnostic.setMessage(message);
        diagnostic.setSeverity(severity);
        diagnostic.setRange(range);
        if (code != null) {
            diagnostic.setCode(code);
        }
        return diagnostic;
    }

    public void validateWithConverter(@NotNull String defValue,
                                      @NotNull PsiType fieldBinding,
                                      @NotNull PsiAnnotationMemberValue defaultValueExpr) {
        DiagnosticSeverity valueSeverity = getSettings().getValidationValueSeverity();
        if (projectRuntime == null || valueSeverity == null) {
            return;
        }
        ExecutionMode preferredMode = getSettings().getMode();

        EnumConstantsProvider.SimpleEnumConstantsProvider provider =
                new EnumConstantsProvider.SimpleEnumConstantsProvider();

        String fqn = toQualifiedTypeString(fieldBinding, provider);

        projectRuntime.validateValue(defValue,
                fqn,
                provider,
                preferredMode,
                (errorMessage, source, code, start, end) -> {
                    addDiagnostic(errorMessage, source, defaultValueExpr,
                            MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE.getCode(),
                            valueSeverity, start + 1, end);
                });
    }

    private static String toQualifiedTypeString(@NotNull PsiType type,
                                                @NotNull EnumConstantsProvider.SimpleEnumConstantsProvider provider) {

        if (type instanceof PsiPrimitiveType) {
            return type.getPresentableText(); // int, char, etc.
        }

        if (type instanceof PsiArrayType arrayType) {
            return toQualifiedTypeString(arrayType.getComponentType(), provider) + "[]";
        }

        if (type instanceof PsiClassType classType) {
            PsiClass psiClass = classType.resolve();
            if (psiClass == null) {
                return type.getCanonicalText();
            }

            String qualifiedName = getBinaryName(psiClass);
            if (qualifiedName == null) {
                qualifiedName = psiClass.getName();
            }

            // ENUM SUPPORT
            if (psiClass.isEnum()) {
                List<String> enumConstants = new ArrayList<>();
                for (PsiField field : psiClass.getFields()) {
                    if (field instanceof PsiEnumConstant enumConst) {
                        enumConstants.add(enumConst.getName());
                    }
                }
                provider.addEnumConstants(qualifiedName, enumConstants);
            }

            // GENERIC TYPE ARGUMENTS
            PsiType[] parameters = classType.getParameters();
            if (parameters.length == 0) {
                return qualifiedName;
            }

            StringBuilder sb = new StringBuilder(qualifiedName);
            sb.append("<");
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(toQualifiedTypeString(parameters[i], provider));
            }
            sb.append(">");
            return sb.toString();
        }

        return type.getCanonicalText();
    }

    /**
     * Returns the JVM binary name of a PsiClass.
     *
     * Example:
     *   - Top-level class: com.example.Foo            -> com.example.Foo
     *   - Inner class:     com.example.Outer.Inner    -> com.example.Outer$Inner
     *   - Nested inner:    Outer.Inner.Deep           -> com.example.Outer$Inner$Deep
     *
     * This replicates the behavior of JDT's ITypeBinding.getBinaryName().
     */
    private static @Nullable String getBinaryName(@NotNull PsiClass psiClass) {
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            return null;
        }

        // If the class is top-level, the qualified name is already correct.
        PsiClass outerClass = psiClass.getContainingClass();
        if (outerClass == null) {
            return qualifiedName;
        }

        // Build the binary name by collecting all containing classes
        // and joining them using '$'.
        List<String> classNames = new ArrayList<>();
        PsiClass current = psiClass;

        while (current != null) {
            String name = current.getName();
            if (name != null) {
                classNames.add(name);
            }
            current = current.getContainingClass();
        }

        // Reverse to get Outer -> Inner -> Deep
        Collections.reverse(classNames);

        // Extract the package prefix (everything before the first class name)
        int lastDot = qualifiedName.indexOf(classNames.get(0));
        String packagePrefix = lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";

        // Combine package and nested class names using '$'
        return packagePrefix + String.join("$", classNames);
    }

    public @NotNull List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }
}
