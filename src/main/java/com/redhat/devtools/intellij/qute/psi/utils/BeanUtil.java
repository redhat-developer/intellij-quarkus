package com.redhat.devtools.intellij.qute.psi.utils;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

/**
 * 
 * @author V. Kabanovich
 *
 */
public class BeanUtil {
	public static final String GET = "get"; //$NON-NLS-1$
	public static final String SET = "set"; //$NON-NLS-1$
	public static final String IS = "is"; //$NON-NLS-1$

	public static boolean isGetter(String methodName, int numberOfParameters) {
		return (((methodName.startsWith(GET) && !methodName.equals(GET))
				|| (methodName.startsWith(IS) && !methodName.equals(IS))) && numberOfParameters == 0);
	}

	public static boolean isSetter(String methodName, int numberOfParameters) {
		return (((methodName.startsWith(SET) && !methodName.equals(SET))) && numberOfParameters == 1);
	}

	public static boolean isGetter(PsiMethod method) {
		return method != null && isGetter(method.getName(), method.getParameterList().getParametersCount())
				&& checkPropertyReturnType(method);
	}

	public static boolean checkPropertyReturnType(String typeName, String methodName) {
		if (typeName == null || typeName.equals("void")) { //$NON-NLS-1$
			return false;
		}
		if (methodName.startsWith(BeanUtil.IS)) {
			if (!"boolean".equals(typeName) && !"java.lang.Boolean".equals(typeName)) { //$NON-NLS-1$ //$NON-NLS-2$
				return false;
			}
		}
		return true;
	}

	private static boolean checkPropertyReturnType(PsiMethod method) {
		return method != null && checkPropertyReturnType(getMemberTypeAsString(method), method.getName());
	}

	public static boolean isSetter(PsiMethod method) {
		return method != null && isSetter(method.getName(), method.getParameterList().getParametersCount());
	}

	public static String getPropertyName(String methodName) {
		if (isGetter(methodName, 0) || isSetter(methodName, 1)) {
			StringBuffer name = new StringBuffer(methodName);
			if (methodName.startsWith(IS)) {
				name.delete(0, 2);
			} else {
				name.delete(0, 3);
			}
			if (name.length() < 2 || !Character.isUpperCase(name.charAt(1))) {
				name.setCharAt(0, Character.toLowerCase(name.charAt(0)));
			}
			return name.toString();
		}
		return null;
	}

	/**
	 * Converts Java Class Name to name of Bean
	 * 
	 * @param className is short name or fully qualified name
	 * @return Bean Name
	 */
	public static String getDefaultBeanName(String className) {
		int lastDotPosition = className.lastIndexOf("."); //$NON-NLS-1$
		if (lastDotPosition >= 0 && className.length() > lastDotPosition) {
			className = className.substring(lastDotPosition + 1);
		}
		if (className.length() > 0) {
			className = className.substring(0, 1).toLowerCase() + className.substring(1);
		}
		return className;
	}

	/**
	 * Returns name of Bean for the given IType
	 * 
	 * @param type
	 * @return Bean Name
	 */
	public static String getDefaultBeanName(PsiClass type) {
		return getDefaultBeanName(type.getName());
	}

	/**
	 * Converts name of Bean to Java Class Name
	 * 
	 * @param beanName is short name or fully qualified name
	 * @return Java Class Name
	 */
	public static String getClassName(String beanName) {
		int lastDotPosition = beanName.lastIndexOf("."); //$NON-NLS-1$
		String beforeLastDot = "";
		if (lastDotPosition >= 0 && beanName.length() > lastDotPosition) {
			beforeLastDot = beanName.substring(0, lastDotPosition + 1);
			lastDotPosition++;
		} else {
			lastDotPosition = 0;
		}
		if (beanName.length() > lastDotPosition) {
			beanName = beforeLastDot + beanName.substring(lastDotPosition, lastDotPosition + 1).toUpperCase()
					+ beanName.substring(lastDotPosition + 1);
		}
		return beanName;
	}

	private static String getMemberTypeAsString(PsiMethod m) {
		if (m == null)
			return null;
		try {
			return resolveTypeAsString(m.getContainingClass(), m.getReturnType());
		} catch (RuntimeException e) {
			// CommonCorePlugin.getPluginLog().logError(e);
		}
		return null;
	}

	private static String resolveTypeAsString(PsiClass type, PsiType typeName) {
		if (type == null || typeName == null)
			return null;
		//return resolveType(type, typeName.getCanonicalText());
		//TODO: check for generics
		return typeName.getCanonicalText();
	}

}