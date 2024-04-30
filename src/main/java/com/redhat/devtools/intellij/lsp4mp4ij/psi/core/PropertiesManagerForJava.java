/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens.IJavaCodeLensParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens.JavaCodeLensContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.completion.IJavaCompletionParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.completion.JavaCompletionContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.definition.IJavaDefinitionParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.definition.JavaDefinitionContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaDiagnosticsParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.hover.IJavaHoverParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.hover.JavaHoverContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.java.codeaction.CodeActionHandler;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4mp.commons.*;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JDT quarkus manager for Java files.
 *
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/PropertiesManagerForJava.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/PropertiesManagerForJava.java</a>
 *
 */
public class PropertiesManagerForJava {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesManagerForJava.class);

    private static final PropertiesManagerForJava INSTANCE = new PropertiesManagerForJava();

    public static PropertiesManagerForJava getInstance() {
        return INSTANCE;
    }

    private final CodeActionHandler codeActionHandler;

    private PropertiesManagerForJava() {
        this.codeActionHandler = new CodeActionHandler();
    }

    /**
     * Returns the Java file information (ex : package name) from the given file URI
     * and null otherwise.
     *
     * @param params  the file information parameters.
     * @param utils   the utilities class
     * @return the Java file information (ex : package name) from the given file URI
     *         and null otherwise.
     */
    public JavaFileInfo fileInfo(MicroProfileJavaFileInfoParams params, IPsiUtils utils) {
        String uri = params.getUri();
        final PsiFile unit = utils.resolveCompilationUnit(uri);
        if (unit != null && unit.isValid() && unit instanceof PsiJavaFile) {
            JavaFileInfo fileInfo = new JavaFileInfo();
            String packageName = ((PsiJavaFile) unit).getPackageName();
            fileInfo.setPackageName(packageName);
            return fileInfo;
        }
        return null;
    }

    /**
     * Returns the codelens list according the given codelens parameters.
     *
     * @param params  the codelens parameters
     * @param utils   the utilities class
     * @return the codelens list according the given codelens parameters.
     */
    public List<? extends CodeLens> codeLens(MicroProfileJavaCodeLensParams params, IPsiUtils utils,  ProgressIndicator monitor) {
        String uri = params.getUri();
        PsiFile typeRoot = resolveTypeRoot(uri, utils);
        if (typeRoot == null) {
            return Collections.emptyList();
        }
        List<CodeLens> lenses = new ArrayList<>();
        collectCodeLens(uri, typeRoot, utils, params, lenses, monitor);
        return lenses;
    }

    private void collectCodeLens(String uri, PsiFile typeRoot, IPsiUtils utils, MicroProfileJavaCodeLensParams params,
                                 List<CodeLens> lenses, ProgressIndicator monitor) {
        // Collect all adapted codeLens participant
        try {
            Module module = utils.getModule(uri);
            if (module == null) {
                return;
            }
            JavaCodeLensContext context = new JavaCodeLensContext(uri, typeRoot, utils, module, params);
            List<IJavaCodeLensParticipant> definitions = IJavaCodeLensParticipant.EP_NAME.getExtensionList()
                    .stream().filter(definition -> definition.isAdaptedForCodeLens(context, monitor))
                    .collect(Collectors.toList());
            if (definitions.isEmpty()) {
                return;
            }

            // Begin, collect, end participants
            definitions.forEach(definition -> definition.beginCodeLens(context, monitor));
            definitions.forEach(definition -> {
                List<CodeLens> collectedLenses = definition.collectCodeLens(context, monitor);
                if (collectedLenses != null && !collectedLenses.isEmpty()) {
                    lenses.addAll(collectedLenses);
                }
            });
            definitions.forEach(definition -> definition.endCodeLens(context, monitor));
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Returns the CompletionItems given the completion item params
     *
     * @param params  the completion item params
     * @param utils   the IJDTUtils
     * @return the CompletionItems for the given the completion item params
     */
    public CompletionList completion(MicroProfileJavaCompletionParams params, IPsiUtils utils) {
        try {
            String uri = params.getUri();
            PsiFile typeRoot = resolveTypeRoot(uri, utils);
            if (typeRoot == null) {
                return null;
            }

            Module module = utils.getModule(uri);
            if (module == null) {
                return null;
            }

            Position completionPosition = params.getPosition();
            int completionOffset = utils.toOffset(typeRoot, completionPosition.getLine(),
                    completionPosition.getCharacter());

            List<CompletionItem> completionItems = new ArrayList<>();
            JavaCompletionContext completionContext = new JavaCompletionContext(uri, typeRoot, utils, module, completionOffset);

            List<IJavaCompletionParticipant> completions = IJavaCompletionParticipant.EP_NAME.extensions()
                    .filter(completion -> completion.isAdaptedForCompletion(completionContext))
                    .collect(Collectors.toList());

            if (completions.isEmpty()) {
                return null;
            }

            completions.forEach(completion -> {
                List<? extends CompletionItem> collectedCompletionItems = completion.collectCompletionItems(completionContext);
                if (collectedCompletionItems != null) {
                    completionItems.addAll(collectedCompletionItems);
                }
            });

            CompletionList completionList = new CompletionList();
            completionList.setItems(completionItems);
            return completionList;
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
            return null;
        }
    }

    /**
     * Returns the definition list according the given definition parameters.
     *
     * @param params  the definition parameters
     * @param utils   the utilities class
     * @return the definition list according the given definition parameters.
     */
    public List<MicroProfileDefinition> definition(MicroProfileJavaDefinitionParams params, IPsiUtils utils) {
        String uri = params.getUri();
        PsiFile typeRoot = resolveTypeRoot(uri, utils);
        if (typeRoot == null) {
            return Collections.emptyList();
        }

        Position hyperlinkedPosition = params.getPosition();
        int definitionOffset = utils.toOffset(typeRoot, hyperlinkedPosition.getLine(),
                hyperlinkedPosition.getCharacter());
        PsiElement hyperlinkedElement = getHoveredElement(typeRoot, definitionOffset);

        List<MicroProfileDefinition> locations = new ArrayList<>();
        collectDefinition(uri, typeRoot, hyperlinkedElement, utils, hyperlinkedPosition, locations);
        return locations;
    }

    private void collectDefinition(String uri, PsiFile typeRoot, PsiElement hyperlinkedElement, IPsiUtils utils,
                                   Position hyperlinkedPosition, List<MicroProfileDefinition> locations) {
        VirtualFile file = null;
        try {
            file = utils.findFile(uri);
            if (file != null) {
                Module module = utils.getModule(file);
                if (module != null) {
                    // Collect all adapted definition participant
                    JavaDefinitionContext context = new JavaDefinitionContext(uri, typeRoot, utils, module,
                            hyperlinkedElement, hyperlinkedPosition);
                    List<IJavaDefinitionParticipant> definitions = IJavaDefinitionParticipant.EP_NAME.extensions()
                            .filter(definition -> definition.isAdaptedForDefinition(context))
                            .toList();
                    if (definitions.isEmpty()) {
                        return;
                    }

                    // Begin, collect, end participants
                    definitions.forEach(definition -> definition.beginDefinition(context));
                    definitions.forEach(definition -> {
                        List<MicroProfileDefinition> collectedDefinitions = definition.collectDefinitions(context);
                        if (collectedDefinitions != null && !collectedDefinitions.isEmpty()) {
                            locations.addAll(collectedDefinitions);
                        }
                    });
                    definitions.forEach(definition -> definition.endDefinition(context));
                }
            }
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }


    /**
     * Returns diagnostics for the given uris list.
     *
     * @param params the diagnostics parameters
     * @param utils  the utilities class
     * @return diagnostics for the given uris list.
     */
    public List<PublishDiagnosticsParams> diagnostics(MicroProfileJavaDiagnosticsParams params, IPsiUtils utils) {
        List<String> uris = params.getUris();
        if (uris == null) {
            return Collections.emptyList();
        }
        DocumentFormat documentFormat = params.getDocumentFormat();
        List<PublishDiagnosticsParams> publishDiagnostics = new ArrayList<PublishDiagnosticsParams>();
        for (String uri : uris) {
            List<Diagnostic> diagnostics = new ArrayList<>();
            PublishDiagnosticsParams publishDiagnostic = new PublishDiagnosticsParams(uri, diagnostics);
            publishDiagnostics.add(publishDiagnostic);
            collectDiagnostics(uri, utils, documentFormat, params.getSettings(), diagnostics);
        }
        return publishDiagnostics;
    }

    private void collectDiagnostics(String uri, IPsiUtils utils, DocumentFormat documentFormat,
                                    MicroProfileJavaDiagnosticsSettings settings, List<Diagnostic> diagnostics) {
        PsiFile typeRoot = resolveTypeRoot(uri, utils);
        if (typeRoot == null) {
            return;
        }

        try {
            Module module = utils.getModule(uri);
                // Collect all adapted diagnostics participant
                JavaDiagnosticsContext context = new JavaDiagnosticsContext(uri, typeRoot, utils, module, documentFormat, settings);
                List<IJavaDiagnosticsParticipant> definitions = IJavaDiagnosticsParticipant.EP_NAME.extensions()
                        .filter(definition -> definition.isAdaptedForDiagnostics(context))
                        .toList();
                if (definitions.isEmpty()) {
                    return;
                }

                // Begin, collect, end participants
                definitions.forEach(definition -> definition.beginDiagnostics(context));
                definitions.forEach(definition -> {
                    List<Diagnostic> collectedDiagnostics = definition.collectDiagnostics(context);
                    if (collectedDiagnostics != null && !collectedDiagnostics.isEmpty()) {
                        diagnostics.addAll(collectedDiagnostics);
                    }
                });
                definitions.forEach(definition -> definition.endDiagnostics(context));
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Returns the hover information according to the given <code>params</code>
     *
     * @param params  the hover parameters
     * @param utils   the utilities class
     * @return the hover information according to the given <code>params</code>
     */
    public Hover hover(MicroProfileJavaHoverParams params, IPsiUtils utils) {
        String uri = params.getUri();
        PsiFile typeRoot = resolveTypeRoot(uri, utils);
        if (typeRoot == null) {
            return null;
        }
        Document document = PsiDocumentManager.getInstance(typeRoot.getProject()).getDocument(typeRoot);
        if (document == null) {
            return null;
        }
        Position hoverPosition = params.getPosition();
        int hoveredOffset = utils.toOffset(document, hoverPosition.getLine(), hoverPosition.getCharacter());
        PsiElement hoverElement = getHoveredElement(typeRoot, hoveredOffset);
        if (hoverElement == null) return null;

        DocumentFormat documentFormat = params.getDocumentFormat();
        boolean surroundEqualsWithSpaces = params.isSurroundEqualsWithSpaces();
        List<Hover> hovers = new ArrayList<>();
        collectHover(uri, typeRoot, hoverElement, utils, hoverPosition, documentFormat, surroundEqualsWithSpaces,
                hovers);
        if (hovers.isEmpty()) {
            return null;
        }
        // TODO : aggregate the hover
        return hovers.get(0);
    }

    /**
     * Returns the cursor context for the given file and cursor position.
     *
     * @param params  the completion params that provide the file and cursor
     *                position to get the context for
     * @param utils   the jdt utils
     * @return the cursor context for the given file and cursor position
     */
    public JavaCursorContextResult javaCursorContext(MicroProfileJavaCompletionParams params, IPsiUtils utils) {
        String uri = params.getUri();
        PsiFile typeRoot = resolveTypeRoot(uri, utils);
        if (!(typeRoot instanceof PsiJavaFile)) {
            return new JavaCursorContextResult(JavaCursorContextKind.IN_EMPTY_FILE, "");
        }
        Document document = PsiDocumentManager.getInstance(typeRoot.getProject()).getDocument(typeRoot);
        if (document == null) {
            return new JavaCursorContextResult(JavaCursorContextKind.IN_EMPTY_FILE, "");
        }
        Position completionPosition = params.getPosition();
        int completionOffset = utils.toOffset(document, completionPosition.getLine(), completionPosition.getCharacter());

        JavaCursorContextKind kind = getJavaCursorContextKind((PsiJavaFile) typeRoot, completionOffset);
        String prefix = getJavaCursorPrefix(document, completionOffset);

        return new JavaCursorContextResult(kind, prefix);
    }

    private static @NotNull JavaCursorContextKind getJavaCursorContextKind(PsiJavaFile javaFile, int completionOffset) {
        if (javaFile.getClasses().length == 0) {
            return JavaCursorContextKind.IN_EMPTY_FILE;
        }

        PsiElement element = javaFile.findElementAt(completionOffset);
        PsiElement parent = PsiTreeUtil.getParentOfType(element, PsiModifierListOwner.class);

        if (parent == null) {
            // We are likely before or after the class declaration
            PsiElement firstClass = javaFile.getClasses()[0];

            if (completionOffset <= firstClass.getTextOffset()) {
                return JavaCursorContextKind.BEFORE_CLASS;
            }

            return JavaCursorContextKind.NONE;
        }

        if (parent instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) parent;
            return getContextKindFromClass(completionOffset, psiClass, element);
        }
        if (parent instanceof PsiAnnotation) {
            PsiAnnotation psiAnnotation = (PsiAnnotation) parent;
            @Nullable PsiAnnotationOwner annotationOwner = psiAnnotation.getOwner();
            if (annotationOwner instanceof PsiClass) {
                return (psiAnnotation.getStartOffsetInParent() == 0)? JavaCursorContextKind.BEFORE_CLASS:JavaCursorContextKind.IN_CLASS_ANNOTATIONS;
            }
            if (annotationOwner instanceof PsiMethod){
                return (psiAnnotation.getStartOffsetInParent() == 0)? JavaCursorContextKind.BEFORE_METHOD:JavaCursorContextKind.IN_METHOD_ANNOTATIONS;
            }
            if (annotationOwner instanceof PsiField) {
                return (psiAnnotation.getStartOffsetInParent() == 0)? JavaCursorContextKind.BEFORE_FIELD:JavaCursorContextKind.IN_FIELD_ANNOTATIONS;
            }
        }
        if (parent instanceof PsiMethod) {
            PsiMethod psiMethod = (PsiMethod) parent;
            if (completionOffset == psiMethod.getTextRange().getStartOffset()) {
                return JavaCursorContextKind.BEFORE_METHOD;
            }
            int methodStartOffset = getMethodStartOffset(psiMethod);
            if (completionOffset <= methodStartOffset) {
                if (psiMethod.getAnnotations().length > 0) {
                    return JavaCursorContextKind.IN_METHOD_ANNOTATIONS;
                }
                return JavaCursorContextKind.BEFORE_METHOD;
            }
        }

        if (parent instanceof PsiField) {
            PsiField psiField = (PsiField) parent;
            if (completionOffset == psiField.getTextRange().getStartOffset()) {
                return JavaCursorContextKind.BEFORE_FIELD;
            }
            int fieldStartOffset = getFieldStartOffset(psiField);
            if (completionOffset <= fieldStartOffset) {
                if (psiField.getAnnotations().length > 0) {
                    return JavaCursorContextKind.IN_FIELD_ANNOTATIONS;
                }
                return JavaCursorContextKind.BEFORE_FIELD;
            }
        }

        return JavaCursorContextKind.NONE;
    }

    @NotNull
    private static JavaCursorContextKind getContextKindFromClass(int completionOffset, PsiClass psiClass, PsiElement element) {
        if (completionOffset <= psiClass.getTextRange().getStartOffset()) {
            return JavaCursorContextKind.BEFORE_CLASS;
        }
        int classStartOffset = getClassStartOffset(psiClass);
        if (completionOffset <= classStartOffset) {
            if (psiClass.getAnnotations().length > 0) {
                return JavaCursorContextKind.IN_CLASS_ANNOTATIONS;
            }
            return JavaCursorContextKind.BEFORE_CLASS;
        }

        PsiElement nextElement = element.getNextSibling();

        if (nextElement instanceof  PsiField) {
            return JavaCursorContextKind.BEFORE_FIELD;
        }
        if (nextElement instanceof  PsiMethod) {
            return JavaCursorContextKind.BEFORE_METHOD;
        }
        if (nextElement instanceof  PsiClass) {
            return JavaCursorContextKind.BEFORE_CLASS;
        }

        return JavaCursorContextKind.IN_CLASS;
    }

    private static @NotNull String getJavaCursorPrefix(@NotNull Document document, int completionOffset) {
        String fileContents = document.getText();
        int i;
        for (i = completionOffset; i > 0 && !Character.isWhitespace(fileContents.charAt(i - 1)); i--) {
        }
        return fileContents.substring(i, completionOffset);
    }

    private static int getMethodStartOffset(PsiMethod psiMethod) {
        int startOffset = psiMethod.getTextOffset();

        int modifierStartOffset = getFirstKeywordOffset(psiMethod);
        if (modifierStartOffset > -1) {
            return Math.min(startOffset, modifierStartOffset);
        }

        PsiTypeElement returnTypeElement = psiMethod.getReturnTypeElement();
        if (returnTypeElement != null) {
            int returnTypeEndOffset = returnTypeElement.getTextRange().getStartOffset();
            startOffset = Math.min(startOffset, returnTypeEndOffset);
        }

        return startOffset;
    }

    private static int getClassStartOffset(PsiClass psiClass) {
        int startOffset = psiClass.getTextOffset();

        int modifierStartOffset = getFirstKeywordOffset(psiClass);
        if (modifierStartOffset > -1) {
            return Math.min(startOffset, modifierStartOffset);
        }
        return startOffset;
    }

    private static int getFieldStartOffset(PsiField psiField) {
        int startOffset = psiField.getTextOffset();

        int modifierStartOffset = getFirstKeywordOffset(psiField);
        if (modifierStartOffset > -1) {
            return Math.min(startOffset, modifierStartOffset);
        }

        PsiTypeElement typeElement = psiField.getTypeElement();
        if (typeElement != null) {
            int typeElementOffset = typeElement.getTextRange().getStartOffset();
            startOffset = Math.min(startOffset, typeElementOffset);
        }

        return startOffset;
    }

    private static int getFirstKeywordOffset(PsiModifierListOwner modifierOwner) {
        PsiModifierList modifierList = modifierOwner.getModifierList();
        if (modifierList != null) {
            PsiElement[] modifiers = modifierList.getChildren();
            for (PsiElement modifier : modifiers) {
                if (modifier instanceof PsiKeyword) {
                    return modifier.getTextRange().getStartOffset();
                }
            }
        }
        return -1;
    }

    @Nullable
    private PsiElement getHoveredElement(PsiFile typeRoot, int offset) {
        PsiElement hoverElement = typeRoot.findElementAt(offset);
        if (hoverElement == null) {
            return null;
        }
        hoverElement = PsiTreeUtil.getParentOfType(hoverElement, PsiModifierListOwner.class);
        if (hoverElement instanceof PsiMethod) {
            hoverElement = getHoveredMethodParameter((PsiMethod) hoverElement, offset);
        }
        return hoverElement;
    }

    /**
     * Returns the parameter element from the given <code>method</code> that
     * contains the given <code>offset</code>.
     *
     * Returns the given <code>method</code> if the correct parameter element cannot
     * be found
     *
     * @param method the method
     * @param offset the offset
     * @return the parameter element from the given <code>method</code> that
     *         contains the given <code>offset</code>
     */
    private PsiElement getHoveredMethodParameter(PsiMethod method, int offset) {
        JvmParameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] instanceof PsiParameter) {
                int start = ((PsiParameter)parameters[i] ).getStartOffsetInParent();
                int end = start + ((PsiParameter) parameters[i]).getTextLength();
                if (start <= offset && offset <= end) {
                    return (PsiElement) parameters[i];
                }
            }
        }
        return method;
    }

    private void collectHover(String uri, PsiFile typeRoot, PsiElement hoverElement, IPsiUtils utils,
                              Position hoverPosition, DocumentFormat documentFormat, boolean surroundEqualsWithSpaces,
                              List<Hover> hovers) {
        try {
            VirtualFile file = utils.findFile(uri);
            if (file != null) {
                Module module = utils.getModule(file);
                if (module != null) {
                    // Collect all adapted hover participant
                    JavaHoverContext context = new JavaHoverContext(uri, typeRoot, utils, module, hoverElement, hoverPosition,
                            documentFormat, surroundEqualsWithSpaces);
                    List<IJavaHoverParticipant> definitions = IJavaHoverParticipant.EP_NAME.extensions()
                            .filter(definition -> definition.isAdaptedForHover(context)).collect(Collectors.toList());
                    if (definitions.isEmpty()) {
                        return;
                    }

                    // Begin, collect, end participants
                    definitions.forEach(definition -> definition.beginHover(context));
                    definitions.forEach(definition -> {
                        Hover hover = definition.collectHover(context);
                        if (hover != null) {
                            hovers.add(hover);
                        }
                    });
                    definitions.forEach(definition -> definition.endHover(context));
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
    }


    /**
     * Given the uri returns a {@link PsiFile}. May return null if it can not
     * associate the uri with a Java file ot class file.
     *
     * @param uri
     * @param utils   JDT LS utilities
     * @return compilation unit
     */
    private static PsiFile resolveTypeRoot(String uri, IPsiUtils utils) {
        return utils.resolveCompilationUnit(uri);
    }

    /**
     * Returns the codeAction list according the given codeAction parameters.
     *
     * @param params  the codeAction parameters
     * @param utils   the utilities class
     * @return the codeAction list according the given codeAction parameters.
     */
    public List<? extends CodeAction> codeAction(MicroProfileJavaCodeActionParams params, IPsiUtils utils) {
        return codeActionHandler.codeAction(params, utils);
    }

    /**
     * Returns the codeAction list according the given codeAction parameters.
     *
     * @param unresolved the CodeAction to resolve
     * @param utils      the utilities class
     * @return the codeAction list according the given codeAction parameters.
     */
    public CodeAction resolveCodeAction(CodeAction unresolved, IPsiUtils utils) {
        return codeActionHandler.resolveCodeAction(unresolved, utils);
    }

}
