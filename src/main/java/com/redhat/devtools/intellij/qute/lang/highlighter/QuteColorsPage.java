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
package com.redhat.devtools.intellij.qute.lang.highlighter;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.redhat.devtools.intellij.qute.QuteBundle;
import com.redhat.devtools.intellij.qute.lang.QuteFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

/**
 * Qute color settings page.
 */
public class QuteColorsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] ATTRS = new AttributesDescriptor[]{
            new AttributesDescriptor(QuteBundle.message("options.qute.attribute.descriptor.comment"), QuteHighlighterColors.COMMENT),
            new AttributesDescriptor(QuteBundle.message("options.qute.attribute.descriptor.edge"), QuteHighlighterColors.QUTE_EDGE),
            new AttributesDescriptor(QuteBundle.message("options.qute.attribute.descriptor.tag"), QuteHighlighterColors.SECTION_TAG_NAME),
            new AttributesDescriptor(QuteBundle.message("options.qute.attribute.descriptor.string"), QuteHighlighterColors.STRING),
            new AttributesDescriptor(QuteBundle.message("options.qute.attribute.descriptor.numeric"), QuteHighlighterColors.NUMERIC),
            new AttributesDescriptor(QuteBundle.message("options.qute.attribute.descriptor.boolean"), QuteHighlighterColors.BOOLEAN),
            new AttributesDescriptor(QuteBundle.message("options.qute.attribute.descriptor.keyword"), QuteHighlighterColors.KEYWORD)
    };

    @Override
    @NotNull
    public String getDisplayName() {
        return QuteBundle.message("options.qute.display.name");
    }

    @Override
    public Icon getIcon() {
        return QuteFileType.QUTE.getIcon();
    }

    @Override
    public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
        return ATTRS;
    }

    @Override
    public ColorDescriptor @NotNull [] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    @NotNull
    public SyntaxHighlighter getHighlighter() {
        return new QuteSyntaxHighlighter();
    }

    @Override
    @NotNull
    public String getDemoText() {
        return "{! See following sample at https://quarkus.io/guides/qute-reference#expression_resolution !}\n" +
                "<html>\n" +
                "{item.name} \n" +
                "<ul>\n" +
                "{#for item in item.derivedItems} \n" +
                "  <li>\n" +
                "  {item.name} \n" +
                "  is derived from\n" +
                "  {data:item.name} \n" +
                "  </li>\n" +
                "{/for}\n" +
                "</ul>\n" +
                "</html>";
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }
}
