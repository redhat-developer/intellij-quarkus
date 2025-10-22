package com.redhat.devtools.intellij.qute.lang;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.redhat.devtools.intellij.qute.lang.psi.QuteElementType;
import com.redhat.devtools.intellij.qute.lang.psi.QuteElementTypes;
import com.redhat.devtools.intellij.qute.lang.psi.QuteTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuteASTMethodPart extends QuteASTPart {

    public QuteASTMethodPart(CharSequence text) {
        super(QuteTokenType.QUTE_EXPRESSION_METHOD_PART, text);
    }

    public @Nullable PsiElement getRootPart() {
        PsiElement part = this;
        if (isQuteType(super.getParent(), QuteTokenType.QUTE_EXPRESSION_METHOD_PART)) {
            part = super.getParent();
        }
        QuteASTObjectPart objectPart = findObjectPart(part);
        if (objectPart != null) {
            return objectPart.getRootPart();
        }
        return null;
    }

    @Override
    protected @Nullable TextRange getTextRangeInExpression(@NotNull PsiElement root) {
        var closedBracket = findClosedBracket(this);
        return new TextRange(root.getTextRange().getStartOffset(), closedBracket != null ? closedBracket.getTextRange().getEndOffset() :  super.getTextRange().getEndOffset());
    }

    protected @Nullable PsiElement findClosedBracket(PsiElement part) {
        var next = part.getNextSibling();
        if (!isQuteType(next, QuteTokenType.QUTE_EXPRESSION_OPEN_BRACKET)) {
            return null;
        }
        int nbBrackets = 0;
        while(next != null) {
            if (isQuteType(next, QuteTokenType.QUTE_EXPRESSION_OPEN_BRACKET)) {
                nbBrackets++;
            } else  if (isQuteType(next, QuteTokenType.QUTE_EXPRESSION_CLOSE_BRACKET)) {
                nbBrackets--;
                if (nbBrackets == 0) {
                    return next;
                }
            }
            next = next.getNextSibling();
        }
        return null;
    }
}
