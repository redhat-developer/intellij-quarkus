package com.redhat.devtools.intellij.qute.lang;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.tree.IElementType;
import com.redhat.devtools.intellij.qute.lang.psi.QuteToken;
import com.redhat.devtools.intellij.qute.lang.psi.QuteTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class QuteASTPart extends QuteToken {

    public QuteASTPart(@NotNull IElementType type, CharSequence text) {
        super(type, text);
    }

    @Override
    public @Nullable TextRange getTextRangeInExpression() {
        var root = getRootPart();
        if (root == null) {
            return null;
        }
        if (root == this) {
            return getTextRange();
        }
        return getTextRangeInExpression(root);
    }

    protected @Nullable TextRange getTextRangeInExpression(@NotNull PsiElement root) {
        return new TextRange(root.getTextRange().getStartOffset(), super.getTextRange().getEndOffset());
    }

    protected @Nullable QuteASTObjectPart findObjectPart(PsiElement part) {
        var previous = part.getPrevSibling();
        while(previous != null) {
            if (isQuteType(previous, QuteTokenType.QUTE_EXPRESSION_OBJECT_PART)) {
                // ex : uri:Todos
                QuteASTObjectPart objectPart = previous instanceof QuteASTObjectPart o ? o : null;
                if (objectPart == null) {
                    var node = previous.getNode();
                    if (node instanceof CompositeElement) {
                        var firstNode = node.getFirstChildNode();
                        objectPart = firstNode instanceof QuteASTObjectPart o ? o : null;
                    }
                }
                return objectPart;
            } else {
                previous = previous.getPrevSibling();
            }
        }
        return null;
    }

    public abstract @Nullable PsiElement getRootPart();
}
