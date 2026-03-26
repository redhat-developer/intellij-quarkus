package com.redhat.devtools.intellij.qute.lang;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.redhat.devtools.intellij.qute.lang.psi.QuteToken;
import com.redhat.devtools.intellij.qute.lang.psi.QuteTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuteASTPropertyPart extends QuteASTPart {

    public QuteASTPropertyPart(CharSequence text) {
        super(QuteTokenType.QUTE_EXPRESSION_PROPERTY_PART, text);
    }

    public @Nullable PsiElement getRootPart() {
        PsiElement part = this;
        if (isQuteType(super.getParent(), QuteTokenType.QUTE_EXPRESSION_PROPERTY_PART)) {
            part = super.getParent();
        }
        QuteASTObjectPart objectPart = findObjectPart(part);
        if (objectPart != null) {
            return objectPart.getRootPart();
        }
        return null;
    }


}
