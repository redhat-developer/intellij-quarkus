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
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.completion.IJavaCompletionParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.completion.JavaCompletionContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.definition.IJavaDefinitionParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.definition.JavaDefinitionContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaDiagnosticsParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.hover.IJavaHoverParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.hover.JavaHoverContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.JavaFileInfo;
import org.eclipse.lsp4mp.commons.MicroProfileDefinition;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCompletionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDefinitionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaFileInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaHoverParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
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
        return ApplicationManager.getApplication().runReadAction((Computable<JavaFileInfo>) () -> {
            String uri = params.getUri();
            final PsiFile unit = utils.resolveCompilationUnit(uri);
            if (unit != null && unit.isValid() && unit instanceof PsiJavaFile) {
                JavaFileInfo fileInfo = new JavaFileInfo();
                String packageName = ((PsiJavaFile) unit).getPackageName();
                fileInfo.setPackageName(packageName);
                return fileInfo;
            }
            return null;
        });
    }

    /**
     * Returns the CompletionItems given the completion item params
     *
     * @param params  the completion item params
     * @param utils   the IJDTUtils
     * @return the CompletionItems for the given the completion item params
     */
    public CompletionList completion(MicroProfileJavaCompletionParams params, IPsiUtils utils) {
        return ApplicationManager.getApplication().runReadAction((Computable<CompletionList>) () -> {
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
        });
    }

    /**
     * Returns the definition list according the given definition parameters.
     *
     * @param params  the definition parameters
     * @param utils   the utilities class
     * @return the definition list according the given definition parameters.
     */
    public List<MicroProfileDefinition> definition(MicroProfileJavaDefinitionParams params, IPsiUtils utils) {
        return ApplicationManager.getApplication().runReadAction((Computable<List<MicroProfileDefinition>>)() -> {
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
        });
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
                            .collect(Collectors.toList());
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
            collectDiagnostics(uri, utils, documentFormat, diagnostics);
        }
        return publishDiagnostics;
    }

    private void collectDiagnostics(String uri, IPsiUtils utils, DocumentFormat documentFormat,
                                    List<Diagnostic> diagnostics) {
        PsiFile typeRoot = ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) () -> resolveTypeRoot(uri, utils));
        if (typeRoot == null) {
            return;
        }

        try {
            Module module = ApplicationManager.getApplication().runReadAction((ThrowableComputable<Module, IOException>) () -> utils.getModule(uri));
            DumbService.getInstance(module.getProject()).runReadActionInSmartMode(() -> {
                // Collect all adapted diagnostics participant
                JavaDiagnosticsContext context = new JavaDiagnosticsContext(uri, typeRoot, utils, module, documentFormat);
                List<IJavaDiagnosticsParticipant> definitions = IJavaDiagnosticsParticipant.EP_NAME.extensions()
                        .filter(definition -> definition.isAdaptedForDiagnostics(context))
                        .collect(Collectors.toList());
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
            });
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
        return ApplicationManager.getApplication().runReadAction((Computable<Hover>) () -> {
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
        });
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

    public List<? extends CodeAction> codeAction(MicroProfileJavaCodeActionParams params, IPsiUtils utils) {
        return Collections.emptyList();
    }
}
