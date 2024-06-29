// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.redhat.devtools.intellij.qute.lang.format;

import com.intellij.formatting.*;
import com.intellij.formatting.templateLanguages.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.DocumentBasedFormattingModel;
import com.intellij.psi.formatter.FormattingDocumentModelImpl;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.formatter.xml.HtmlPolicy;
import com.intellij.psi.formatter.xml.SyntheticBlock;
import com.intellij.psi.templateLanguages.SimpleTemplateLanguageFormattingModelBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlTag;
import com.redhat.devtools.intellij.qute.lang.psi.QuteElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.intellij.psi.formatter.WrappingUtil.getWrapType;

/**
 * Template aware formatter which provides formatting for Qute syntax and delegates formatting
 * for the templated language to that languages formatter
 * <p>
 * This class is a copy/paste from https://github.com/JetBrains/intellij-plugins/blob/master/Qute/src/com/dmarcotte/Qute/format/HbFormattingModelBuilder.java adapted for Qute.
 */
public class QuteHtmlFormattingModelBuilder extends TemplateLanguageFormattingModelBuilder {


    @Override
    public TemplateLanguageBlock createTemplateLanguageBlock(@NotNull ASTNode node,
                                                             @Nullable Wrap wrap,
                                                             @Nullable Alignment alignment,
                                                             @Nullable List<DataLanguageBlockWrapper> foreignChildren,
                                                             @NotNull CodeStyleSettings codeStyleSettings) {
        final FormattingDocumentModelImpl documentModel = FormattingDocumentModelImpl.createOn(node.getPsi().getContainingFile());
        HtmlPolicy policy = new HtmlPolicy(codeStyleSettings, documentModel);
        return new QuteBlock(node, wrap, alignment, this, codeStyleSettings, foreignChildren, policy);
    }

    /**
     * We have to override {@link TemplateLanguageFormattingModelBuilder#createModel}
     * since after we delegate to some templated languages, those languages (xml/html for sure, potentially others)
     * delegate right back to us to format the HbTokenTypes.OUTER_ELEMENT_TYPE token we tell them to ignore,
     * causing a stack-overflowing loop of polite format-delegation.
     */
    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        final PsiFile file = formattingContext.getContainingFile();
        Block rootBlock;

        ASTNode node = formattingContext.getNode();

        if (node.getElementType() == QuteElementTypes.QUTE_OUTER_ELEMENT_TYPE) {
            // If we're looking at a QuteElementTypes.QUTE_OUTER_ELEMENT_TYPE element, then we've been invoked by our templated
            // language.  Make a dummy block to allow that formatter to continue
            return new SimpleTemplateLanguageFormattingModelBuilder().createModel(formattingContext);
        } else {
            rootBlock = getRootBlock(file, file.getViewProvider(), formattingContext.getCodeStyleSettings());
        }
        return new DocumentBasedFormattingModel(
                rootBlock, formattingContext.getProject(), formattingContext.getCodeStyleSettings(), file.getFileType(), file);
    }

    /**
     * Do format my model!
     *
     * @return false all the time to tell the {@link TemplateLanguageFormattingModelBuilder}
     * to not-not format our model (i.e. yes please!  Format away!)
     */
    @Override
    public boolean dontFormatMyModel() {
        return false;
    }

    private static class QuteBlock extends TemplateLanguageBlock {

        @NotNull
        protected final HtmlPolicy myHtmlPolicy;


        QuteBlock(@NotNull ASTNode node,
                  Wrap wrap,
                  Alignment alignment,
                  @NotNull TemplateLanguageBlockFactory blockFactory,
                  @NotNull CodeStyleSettings settings,
                  @Nullable List<DataLanguageBlockWrapper> foreignChildren,
                  @NotNull HtmlPolicy htmlPolicy) {
            super(node, wrap, alignment, blockFactory, settings, foreignChildren);
            myHtmlPolicy = htmlPolicy;
        }

        @Override
        public Indent getIndent() {
            // ignore whitespace
            if (myNode.getText().trim().isEmpty()) {
                return Indent.getNoneIndent();
            }

            // any element that is the direct descendant of a foreign block gets an indent
            // (unless that foreign element has been configured to not indent its children)
            DataLanguageBlockWrapper foreignParent = getForeignBlockParent(true);
            if (foreignParent != null) {
                if (foreignParent.getNode() instanceof XmlTag
                        && !myHtmlPolicy.indentChildrenOf((XmlTag) foreignParent.getNode())) {
                    return Indent.getNoneIndent();
                }
                return Indent.getNormalIndent();
            }

            return Indent.getNoneIndent();
        }

        @Override
        protected IElementType getTemplateTextElementType() {
            // we ignore CONTENT tokens since they get formatted by the templated language
            return QuteElementTypes.QUTE_TEXT;
        }

        @Override
        public boolean isRequiredRange(TextRange range) {
            // seems our approach doesn't require us to insert any custom DataLanguageBlockFragmentWrapper blocks
            return false;
        }

        /**
         * <p/>
         * This method handles indent and alignment on Enter.
         */
        @NotNull
        @Override
        public ChildAttributes getChildAttributes(int newChildIndex) {
            return new ChildAttributes(Indent.getNoneIndent(), null);
        }


        /**
         * Returns this block's first "real" foreign block parent if it exists, and null otherwise.  (By "real" here, we mean that this method
         * skips SyntheticBlock blocks inserted by the template formatter)
         *
         * @param immediate Pass true to only check for an immediate foreign parent, false to look up the hierarchy.
         */
        private DataLanguageBlockWrapper getForeignBlockParent(boolean immediate) {
            DataLanguageBlockWrapper foreignBlockParent = null;
            BlockWithParent parent = getParent();

            while (parent != null) {
                if (parent instanceof DataLanguageBlockWrapper && !(((DataLanguageBlockWrapper) parent).getOriginal() instanceof SyntheticBlock)) {
                    foreignBlockParent = (DataLanguageBlockWrapper) parent;
                    break;
                } else if (immediate && parent instanceof QuteBlock) {
                    break;
                }
                parent = parent.getParent();
            }

            return foreignBlockParent;
        }
    }
}