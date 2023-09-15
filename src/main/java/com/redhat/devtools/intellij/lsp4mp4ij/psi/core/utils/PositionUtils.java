/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils;

import com.intellij.codeInsight.daemon.impl.analysis.HighlightNamesUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.NotNull;

/**
 * Position utilities.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/PositionUtils.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/PositionUtils.java</a>
 *
 */
public class PositionUtils {

	/**
	 * Returns the LSP range for the given field name.
	 * 
	 * @param field the java field.
	 * @param utils the JDT utilities.
	 * @return the LSP range for the given field name.
	 */
	public static Range toNameRange(PsiField field, IPsiUtils utils) {
		PsiFile openable = field.getContainingFile();
		TextRange sourceRange = field.getNameIdentifier().getTextRange();
		return utils.toRange(openable, sourceRange.getStartOffset(), sourceRange.getLength());
	}

	/**
	 * Returns the LSP range for the given type name.
	 *
	 * @param type  the java type.
	 * @param utils the JDT utilities.
	 * @return the LSP range for the given type name.
	 */
	public static Range toNameRange(PsiClass type, IPsiUtils utils) {
		PsiFile openable = type.getContainingFile();
		TextRange sourceRange = type.getNameIdentifier().getTextRange();
		return utils.toRange(openable, sourceRange.getStartOffset(), sourceRange.getLength());
	}

	/**
	 * Returns the LSP range for the given method name.
	 *
	 * @param method the java type.
	 * @param utils  the JDT utilities.
	 * @return the LSP range for the given method name.
	 */
	public static Range toNameRange(PsiMethod method, IPsiUtils utils) {
		PsiFile openable = method.getContainingFile();
		TextRange sourceRange = method.getNameIdentifier().getTextRange();
		return utils.toRange(openable, sourceRange.getStartOffset(), sourceRange.getLength());
	}

	/**
	 * Returns the LSP Range for the class declaration of the given type
	 *
	 * @param type  the java type.
	 * @param utils the JDT utilities.
	 * @return the LSP range the class declaration of the given type.
	 */
	public static Range toClassDeclarationRange(@NotNull PsiClass type, @NotNull IPsiUtils utils) {
		PsiFile openable = type.getContainingFile();
		TextRange sourceRange = HighlightNamesUtil.getClassDeclarationTextRange(type);
		return utils.toRange(openable, sourceRange.getStartOffset(), sourceRange.getLength());
	}
}
