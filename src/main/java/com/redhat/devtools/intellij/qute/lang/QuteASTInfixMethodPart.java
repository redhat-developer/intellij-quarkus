package com.redhat.devtools.intellij.qute.lang;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.redhat.devtools.intellij.qute.lang.psi.QuteTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuteASTInfixMethodPart extends QuteASTPart {

    public QuteASTInfixMethodPart(CharSequence text) {
        super(QuteTokenType.QUTE_EXPRESSION_INFIX_METHOD_PART, text);
    }

    public @Nullable PsiElement getRootPart() {
        PsiElement part = this;
        if (isQuteType(super.getParent(), QuteTokenType.QUTE_EXPRESSION_INFIX_METHOD_PART)) {
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
        var infixParameter = findInfixParameter(this);
        if (infixParameter == null) {
            return null;
        }
        return new TextRange(root.getTextRange().getStartOffset(), infixParameter.getTextRange().getEndOffset());
    }

    protected @Nullable PsiElement findInfixParameter(PsiElement part) {
        var next = part.getNextSibling();
        if (isQuteType(next, QuteTokenType.QUTE_EXPRESSION_WHITESPACE)) {
            return next.getNextSibling();
        }
        return null;
    }
}
