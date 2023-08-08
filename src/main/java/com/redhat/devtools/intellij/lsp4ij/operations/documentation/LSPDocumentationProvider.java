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
package com.redhat.devtools.intellij.lsp4ij.operations.documentation;

import com.intellij.lang.documentation.DocumentationProviderEx;
import com.intellij.lang.documentation.ExternalDocumentationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LSPVirtualFileWrapper;
import com.redhat.devtools.intellij.lsp4ij.operations.completion.LSPCompletionProposal;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import org.eclipse.lsp4j.MarkupContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link DocumentationProviderEx} implementation for LSP to support:
 *
 * <ul>
 *     <li>textDocument/hover</li>
 *     <li>documentation for completion item</li>
 * </ul>.
 */
public class LSPDocumentationProvider extends DocumentationProviderEx implements ExternalDocumentationHandler {

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

    private static final Key<Integer> TARGET_OFFSET_KEY = new Key<>(LSPDocumentationProvider.class.getName());

    @Nullable
    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        return generateDoc(element, originalElement);
    }

    @Override
    public @Nullable PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file, @Nullable PsiElement contextElement, int targetOffset) {
        if (contextElement != null) {
            // Store the offset where the hover has been triggered
            contextElement.putUserData(TARGET_OFFSET_KEY, targetOffset);
        }
        return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset);
    }

    @Nullable
    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        try {
            Project project = element.getProject();
            if (project.isDisposed()) {
                return null;
            }
            Editor editor = LSPIJUtils.editorForElement(element);
            if (editor == null) {
                return null;
            }
            List<MarkupContent> result = getMarkupContents(element, originalElement);
            if (result == null || result.isEmpty()) {
                return null;
            }
            String s = result
                    .stream()
                    .map(m -> m.getValue())
                    .collect(Collectors.joining("\n\n"));
            return styleHtml(editor, RENDERER.render(PARSER.parse(s)));
        } finally {
            if (originalElement != null) {
                originalElement.putUserData(TARGET_OFFSET_KEY, null);
            }
        }
    }

    @Nullable
    public List<MarkupContent> getMarkupContents(PsiElement element, @Nullable PsiElement originalElement) {
        if (element instanceof LSPPsiElementForLookupItem) {
            // Show documentation for a given completion item in the "documentation popup" (see IJ Completion setting)
            // (LSP textDocument/completion request)
            return ((LSPPsiElementForLookupItem) element).getDocumentation();
        }

        Editor editor = LSPIJUtils.editorForElement(originalElement);
        if (editor == null) {
            return null;
        }

        // Show documentation for a hovered element (LSP textDocument/hover request).
        VirtualFile file = originalElement.getContainingFile().getVirtualFile();
        if (LSPVirtualFileWrapper.hasWrapper(file)) {
            int targetOffset = getTargetOffset(originalElement);
            return LSPVirtualFileWrapper.getLSPVirtualFileWrapper(file).getHoverContent(originalElement, targetOffset, editor);
        }
        return null;
    }

    private static int getTargetOffset(PsiElement originalElement) {
        Integer targetOffset = originalElement.getUserData(TARGET_OFFSET_KEY);
        if (targetOffset != null) {
            return targetOffset;
        }
        int startOffset = originalElement.getTextOffset();
        int textLength = originalElement.getTextLength();
        return startOffset + textLength / 2;
    }

    @Nullable
    @Override
    public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
        if (object instanceof LSPCompletionProposal) {
            MarkupContent documentation = ((LSPCompletionProposal) object).getDocumentation();
            if (documentation != null) {
                return new LSPPsiElementForLookupItem(documentation, psiManager, element);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
        return null;
    }

    @Override
    public boolean handleExternal(PsiElement element, PsiElement originalElement) {
        return false;
    }

    @Override
    public boolean handleExternalLink(PsiManager psiManager, String link, PsiElement context) {
        VirtualFile file = LSPIJUtils.findResourceFor(link);
        if (file != null) {
            FileEditorManager.getInstance(psiManager.getProject()).openFile(file, true, true);
            return true;
        }
        return false;
    }

    @Override
    public boolean canFetchDocumentationLink(String link) {
        return false;
    }

    @Override
    public @NotNull String fetchExternalDocumentation(@NotNull String link, @Nullable PsiElement element) {
        return null;
    }


    public static String styleHtml(Editor editor, String htmlBody) {
        if (htmlBody == null || htmlBody.isEmpty()) {
            return htmlBody;
        }
        Color background = editor.getColorsScheme().getDefaultBackground();
        Color foreground = editor.getColorsScheme().getDefaultForeground();

        StringBuilder html = new StringBuilder("<html><head><style TYPE='text/css'>html { ");
        if (background != null) {
            html.append("background-color: ")
                    .append(toHTMLrgb(background))
                    .append(";");
        }
        if (foreground != null) {
            html.append("color: ")
                    .append(toHTMLrgb(foreground))
                    .append(";");
        }
        html
                .append(" }</style></head><body>")
                .append(htmlBody)
                .append("</body></html>");
        return html.toString();
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
