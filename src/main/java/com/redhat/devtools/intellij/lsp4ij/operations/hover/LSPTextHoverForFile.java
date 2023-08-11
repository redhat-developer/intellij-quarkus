/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij.operations.hover;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.intellij.lsp4ij.internal.CancellationSupport;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * LSP textDocument/hover support for a given file.
 */
public class LSPTextHoverForFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSPTextHoverForFile.class);
    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

    private PsiElement lastElement;
    private int lastOffset = -1;
    private CompletableFuture<List<Hover>> lspRequest;
    private CancellationSupport previousCancellationSupport;

    public String getHoverContent(PsiElement element, int targetOffset, Editor editor) {
        initiateHoverRequest(element, targetOffset);
        try {
            String result = lspRequest.get(500, TimeUnit.MILLISECONDS).stream()
                    .filter(Objects::nonNull)
                    .map(LSPTextHoverForFile::getHoverString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n\n")) //$NON-NLS-1$
                    .trim();
            if (!result.isEmpty()) {
                return styleHtml(editor, RENDERER.render(PARSER.parse(result)));
            }
            // The LSP hover request are finished, don't need to cancel the previous LSP requests.
            previousCancellationSupport = null;
        } catch (ExecutionException | TimeoutException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
            Thread.currentThread().interrupt();
        }
        return null;
    }


    /**
     * Initialize hover requests with hover (if available).
     *
     * @param element the PSI element.
     * @param offset  the target offset.
     */

    private void initiateHoverRequest(PsiElement element, int offset) {
        if (this.previousCancellationSupport != null) {
            // The prvious LSP hover request is not finished,cancel it
            this.previousCancellationSupport.cancel();
        }
        PsiDocumentManager manager = PsiDocumentManager.getInstance(element.getProject());
        final Document document = manager.getDocument(element.getContainingFile());
        if (offset != -1 && (this.lspRequest == null || !element.equals(this.lastElement) || offset != this.lastOffset)) {
            this.lastElement = element;
            this.lastOffset = offset;
            CancellationSupport cancellationSupport = new CancellationSupport();
            this.lspRequest = LanguageServiceAccessor.getInstance(element.getProject())
                    .getLanguageServers(document, capabilities -> isHoverCapable(capabilities))
                    .thenApplyAsync(languageServers -> // Async is very important here, otherwise the LS Client thread is in
                            // deadlock and doesn't read bytes from LS
                            languageServers.stream()
                                    .map(languageServer ->
                                            cancellationSupport.execute(
                                                            languageServer.getServer().getTextDocumentService()
                                                                    .hover(LSPIJUtils.toHoverParams(offset, document)))
                                                    .join()
                                    ).filter(Objects::nonNull).collect(Collectors.toList()));
            // store the current cancel support as previous
            previousCancellationSupport = cancellationSupport;
        }
    }

    private static @Nullable String getHoverString(Hover hover) {
        Either<List<Either<String, MarkedString>>, MarkupContent> hoverContent = hover.getContents();
        if (hoverContent.isLeft()) {
            List<Either<String, MarkedString>> contents = hoverContent.getLeft();
            if (contents == null || contents.isEmpty()) {
                return null;
            }
            return contents.stream().map(content -> {
                if (content.isLeft()) {
                    return content.getLeft();
                } else if (content.isRight()) {
                    MarkedString markedString = content.getRight();
                    // TODO this won't work fully until markup parser will support syntax
                    // highlighting but will help display
                    // strings with language tags, e.g. without it things after <?php tag aren't
                    // displayed
                    if (markedString.getLanguage() != null && !markedString.getLanguage().isEmpty()) {
                        return String.format("```%s%n%s%n```", markedString.getLanguage(), markedString.getValue()); //$NON-NLS-1$
                    } else {
                        return markedString.getValue();
                    }
                } else {
                    return ""; //$NON-NLS-1$
                }
            }).filter(((Predicate<String>) String::isEmpty).negate()).collect(Collectors.joining("\n\n")); //$NON-NLS-1$ )
        } else {
            return hoverContent.getRight().getValue();
        }
    }

    private static boolean isHoverCapable(ServerCapabilities capabilities) {
        return (capabilities.getHoverProvider().isLeft() && capabilities.getHoverProvider().getLeft()) || capabilities.getHoverProvider().isRight();
    }


    public static String styleHtml(Editor editor, String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        Color background = editor.getColorsScheme().getDefaultBackground();
        Color foreground = editor.getColorsScheme().getDefaultForeground();
        // put CSS styling to match Eclipse style
        String style = "<html><head><style TYPE='text/css'>html { " + //$NON-NLS-1$
                (background != null ? "background-color: " + toHTMLrgb(background) + "; " : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                (foreground != null ? "color: " + toHTMLrgb(foreground) + "; " : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                " }</style></head><body>"; //$NON-NLS-1$

        /*int headIndex = html.indexOf(HEAD);
        StringBuilder builder = new StringBuilder(html.length() + style.length());
        builder.append(html.substring(0, headIndex + HEAD.length()));
        builder.append(style);
        builder.append(html.substring(headIndex + HEAD.length()));
        return builder.toString();*/
        StringBuilder builder = new StringBuilder(style);
        builder.append(html).append("</body></html>");
        return builder.toString();
    }

    private static String toHTMLrgb(Color rgb) {
        StringBuilder builder = new StringBuilder(7);
        builder.append('#');
        appendAsHexString(builder, rgb.getRed());
        appendAsHexString(builder, rgb.getGreen());
        appendAsHexString(builder, rgb.getBlue());
        return builder.toString();
    }

    private static void appendAsHexString(StringBuilder buffer, int intValue) {
        String hexValue = Integer.toHexString(intValue);
        if (hexValue.length() == 1) {
            buffer.append('0');
        }
        buffer.append(hexValue);
    }

}
