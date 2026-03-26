/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.run.client;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.dap.DAPDebugProcess;
import com.redhat.devtools.lsp4ij.dap.DebugMode;
import com.redhat.devtools.lsp4ij.dap.client.DAPClient;
import com.redhat.devtools.lsp4ij.settings.ServerTrace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * DAP client extension used by Qute to resolve Java source locations
 * from template-related annotations.
 * <p>
 * This client is able to locate the Java source file and the exact
 * line where a Qute template is defined, based on annotations such as
 * {@code @CheckedTemplate}.
 * <p>
 * The resolution is performed using IntelliJ PSI under a read action
 * and is safe to run asynchronously.
 */
public class QuteDAPClient extends DAPClient implements JavaSourceResolver {

    public QuteDAPClient(@NotNull DAPDebugProcess debugProcess,
                         @NotNull Map<String, Object> dapParameters,
                         boolean isDebug,
                         @NotNull DebugMode debugMode,
                         @NotNull ServerTrace serverTrace,
                         @Nullable DAPClient parentClient) {
        super(debugProcess, dapParameters, isDebug, debugMode, serverTrace, parentClient);
    }

    /**
     * Finds the {@link PsiLiteralExpression} associated with the given
     * annotation.
     * <p>
     * The annotation may be declared:
     * <ul>
     *   <li>on the class itself</li>
     *   <li>on a record component (for Java records)</li>
     *   <li>on a method</li>
     * </ul>
     *
     * @param args    resolution arguments
     * @param project IntelliJ project
     * @return the literal expression of the annotation value,
     * or {@code null} if not found
     */
    private static @Nullable PsiLiteralExpression findLiteralExpression(
            @NotNull JavaSourceLocationArguments args,
            @NotNull Project project) {

        String typeName = args.getTypeName();
        String methodName = args.getMethod(); // optional
        String annotationFqn = args.getAnnotation();

        // Locate the Java class by its fully qualified name
        PsiClass psiClass = ClassUtil.findPsiClass(
                PsiManager.getInstance(project),
                typeName,
                null,
                false,
                GlobalSearchScope.allScope(project)
        );

        if (psiClass == null) {
            return null;
        }

        PsiAnnotation psiAnnotation = null;

        // 1) Annotation declared on the class or record component
        if (methodName == null) {
            psiAnnotation = psiClass.getAnnotation(annotationFqn);

            // For records, the annotation may be declared on a component
            if (psiAnnotation == null && psiClass.isRecord()) {
                for (PsiRecordComponent component : psiClass.getRecordComponents()) {
                    PsiAnnotation a = component.getAnnotation(annotationFqn);
                    if (a != null) {
                        psiAnnotation = a;
                        break;
                    }
                }
            }
        }
        // 2) Annotation declared on a method
        else {
            for (PsiMethod method : psiClass.findMethodsByName(methodName, false)) {
                PsiAnnotation a = method.getAnnotation(annotationFqn);
                if (a != null) {
                    psiAnnotation = a;
                    break;
                }
            }
        }

        if (psiAnnotation == null) {
            return null;
        }

        // Extract the annotation value and ensure it is a literal expression
        PsiAnnotationMemberValue value =
                psiAnnotation.getParameterList().getAttributes()[0].getValue();

        if (value instanceof PsiLiteralExpression literal) {
            return literal;
        }

        return null;
    }

    /**
     * Computes the starting line of the literal content in the source file.
     * <p>
     * If the literal is a Java text block ({@code """}), the returned
     * line corresponds to the first line of the text block content,
     * excluding the opening delimiter and the following newline.
     *
     * @param literal  the literal expression
     * @param document the document containing the literal
     * @return the zero-based line number where the content starts
     */
    private static int getStartLine(@NotNull PsiLiteralExpression literal,
                                    @NotNull Document document) {

        TextRange range = literal.getTextRange();
        String text = literal.getText();
        int startOffset = range.getStartOffset();

        // Adjust the offset to the start of a text block content
        int tripleIndex = text.indexOf("\"\"\"");
        if (tripleIndex != -1) {
            startOffset = range.getStartOffset() + tripleIndex + 3;

            // Skip the newline immediately following the opening """
            if (startOffset < document.getTextLength()) {
                CharSequence chars = document.getCharsSequence();
                char c = chars.charAt(startOffset);

                if (c == '\n') {
                    startOffset++;
                } else if (c == '\r') {
                    startOffset++;
                    if (startOffset < document.getTextLength()
                            && chars.charAt(startOffset) == '\n') {
                        startOffset++;
                    }
                }
            }
        }

        return document.getLineNumber(startOffset);
    }

    /**
     * Resolves the Java source location corresponding to a Qute-related
     * annotation.
     * <p>
     * The method locates the annotated Java element (class, record
     * component or method), extracts the annotation literal value
     * (typically a text block), and computes the line where the
     * template content starts.
     *
     * @param args arguments describing the Java type, method and annotation
     * @return a future completed with the resolved source location,
     * or {@code null} if the source cannot be resolved
     */
    @Override
    public @NotNull CompletableFuture<@Nullable JavaSourceLocationResponse> resolveJavaSource(
            @NotNull JavaSourceLocationArguments args) {

        Project project = getProject();
        CompletableFuture<JavaSourceLocationResponse> result = new CompletableFuture<>();

        // Run PSI access in a non-blocking read action
        ReadAction.nonBlocking(() -> {

            // Locate the literal expression from the requested annotation
            PsiLiteralExpression literal = findLiteralExpression(args, project);
            if (literal == null) {
                result.complete(null);
                return null;
            }

            // Retrieve the document associated with the Java file
            PsiFile file = literal.getContainingFile();
            Document document = PsiDocumentManager.getInstance(project).getDocument(file);
            if (document == null) {
                result.complete(null);
                return null;
            }

            // Build the response with file URI and start line
            JavaSourceLocationResponse response = new JavaSourceLocationResponse();
            response.setJavaFileUri(LSPIJUtils.toUriAsString(file));
            response.setStartLine(getStartLine(literal, document));

            result.complete(response);
            return null;

        }).submit(AppExecutorUtil.getAppExecutorService());

        return result;
    }
}
