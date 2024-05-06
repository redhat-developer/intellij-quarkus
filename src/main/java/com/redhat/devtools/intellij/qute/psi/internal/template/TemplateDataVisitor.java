package com.redhat.devtools.intellij.qute.psi.internal.template;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

public abstract class TemplateDataVisitor extends JavaElementVisitor {

	private static final String DATA_METHOD = "data";

	private PsiMethod method;

	@Override
	public void visitMethodCallExpression(PsiMethodCallExpression node) {
		var resolvedMethod = node.resolveMethod();
		if (resolvedMethod == null) {
			return;
		}
		String methodName = resolvedMethod.getName();
		if (DATA_METHOD.equals(methodName)) {
			// collect the first data method
			// ex : hello.data("height", 1.50, "weight", 50L);
			// will collect data model parameters for "height" and "weight"
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

		// Fluent API support
		PsiMethodCallExpression nextCallExpression = PsiTreeUtil.getParentOfType(node, PsiMethodCallExpression.class);
		if (nextCallExpression != null){
			// collect the other data methods
			// ex : hello.data("height", 1.50, "weight", 50L)
			//		.data("age", 12)
			//		.data("name", name)
			// will collect data model parameters for "age" and "name"
			visitMethodCallExpression(nextCallExpression);
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
