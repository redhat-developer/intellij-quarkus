package com.redhat.devtools.intellij.qute.psi.internal.template;

import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;

public abstract class TemplateDataVisitor extends JavaElementVisitor {

	private static final String DATA_METHOD = "data";

	private PsiMethod method;

	@Override
	public void visitMethodCallExpression(PsiMethodCallExpression node) {
		String methodName = node.resolveMethod().getName();
		if (DATA_METHOD.equals(methodName)) {
			// .data("book", book)
			@SuppressWarnings("rawtypes")
			PsiExpression[] arguments = node.getArgumentList().getExpressions();
			Object paramName = null;
			for (int i = 0; i < arguments.length; i++) {
				if (i % 2 == 0) {
					paramName = null;
					paramName = arguments[i];
				} else {
					if (paramName != null) {
						Object paramType = arguments[i];
						visitParameter(paramName, paramType);
					}
				}
			}
		}
		super.visitMethodCallExpression(node);
	}

	public void setMethod(PsiMethod method) {
		this.method = method;
	}

	public PsiMethod getMethod() {
		return method;
	}
	
	protected abstract boolean visitParameter(Object paramName, Object paramType);

}
