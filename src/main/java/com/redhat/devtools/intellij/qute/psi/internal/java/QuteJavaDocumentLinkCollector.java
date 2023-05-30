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
package com.redhat.devtools.intellij.qute.psi.internal.java;

import java.text.MessageFormat;
import java.util.List;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralValue;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.qute.psi.utils.TemplatePathInfo;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Range;

/**
 * Report document link for opening/creating Qute template for:
 *
 * <ul>
 * <li>declared method which have class annotated with @CheckedTemplate.</li>
 * <li>declared field which have Template as type.</li>
 * </ul>
 *
 * @author Angelo ZERR
 */
public class QuteJavaDocumentLinkCollector extends AbstractQuteTemplateLinkCollector {

    private static final String QUTE_DOCUMENT_LINK_OPEN_URI_MESSAGE = "Open `{0}`";

    private static final String QUTE_DOCUMENT_LINK_GENERATE_TEMPLATE_MESSAGE = "Create `{0}`";

    private final List<DocumentLink> links;

    public QuteJavaDocumentLinkCollector(PsiFile typeRoot, List<DocumentLink> links, IPsiUtils utils,
                                         ProgressIndicator monitor) {
        super(typeRoot, utils, monitor);
        this.links = links;
    }

    @Override
    protected void collectTemplateLink(PsiElement fieldOrMethod, PsiLiteralValue locationAnnotation, PsiClass type, String className,
                                       String fieldOrMethodName, String location, VirtualFile templateFile, TemplatePathInfo templatePathInfo) {
        if (!templatePathInfo.isValid()) {
            // It is an empty fragment which is not valid, don't generate a document link.
            return;
        }
        String templateUri = templateFile != null ? templateFile.getUrl() : getVirtualFileUrl(utils.getModule(), templatePathInfo.getTemplateUri(), PREFERRED_SUFFIX);
        String tooltip = getTooltip(templateFile, templatePathInfo.getTemplateUri());
        Range range = createRange(locationAnnotation != null ? locationAnnotation : fieldOrMethod);
        DocumentLink link = new DocumentLink(range, templateUri, null, tooltip);
        links.add(link);
    }

    private static String getTooltip(VirtualFile templateFile, String templateFilePath) {
        if (templateFile != null) {
            return MessageFormat.format(QUTE_DOCUMENT_LINK_OPEN_URI_MESSAGE, templateFilePath);
        }
        return MessageFormat.format(QUTE_DOCUMENT_LINK_GENERATE_TEMPLATE_MESSAGE, templateFilePath);
    }

}
