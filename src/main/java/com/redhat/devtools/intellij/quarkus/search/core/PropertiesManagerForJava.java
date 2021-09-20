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
package com.redhat.devtools.intellij.quarkus.search.core;

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
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.quarkus.search.core.java.diagnostics.IJavaDiagnosticsParticipant;
import com.redhat.devtools.intellij.quarkus.search.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.quarkus.search.core.java.hover.IJavaHoverParticipant;
import com.redhat.devtools.intellij.quarkus.search.core.java.hover.JavaHoverContext;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.JavaFileInfo;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaFileInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaHoverParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
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
            LOGGER.error(e.getLocalizedMessage(), e);
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
            PsiElement hoverElement = typeRoot.findElementAt(hoveredOffset);
            if (hoverElement == null) {
                return null;
            }

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
}
