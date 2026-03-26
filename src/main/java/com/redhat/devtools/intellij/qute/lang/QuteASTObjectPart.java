package com.redhat.devtools.intellij.qute.lang;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.redhat.devtools.intellij.qute.lang.psi.QuteElementType;
import com.redhat.devtools.intellij.qute.lang.psi.QuteElementTypes;
import com.redhat.devtools.intellij.qute.lang.psi.QuteToken;
import com.redhat.devtools.intellij.qute.lang.psi.QuteTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuteASTObjectPart extends QuteASTPart {

    public QuteASTObjectPart(CharSequence text) {
        super(QuteTokenType.QUTE_EXPRESSION_OBJECT_PART, text);
    }

    public @Nullable PsiElement getRootPart() {
        PsiElement part = this;
        if (isQuteType(super.getParent(), QuteTokenType.QUTE_EXPRESSION_OBJECT_PART)) {
            part = super.getParent();
        }
        var previous = part.getPrevSibling();
        if (isQuteType(previous, QuteTokenType.QUTE_EXPRESSION_COLON_SPACE)) {
            previous = previous.getPrevSibling();
            if (isQuteType(previous, QuteTokenType.QUTE_EXPRESSION_NAMESPACE_PART)) {
                // ex : uri:Todos
                return previous;
            }
        }
        // ex : task
        return this;
    }

}
