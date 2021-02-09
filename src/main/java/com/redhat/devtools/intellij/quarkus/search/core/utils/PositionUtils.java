/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.core.utils;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import org.eclipse.lsp4j.Range;

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
	 * @param field teh java field.
	 * @param utils the JDT utilities.
	 * @return the LSP range for the given field name.
	 */
	public static Range toNameRange(PsiField field, IPsiUtils utils) {
		PsiFile openable = field.getContainingFile();
		TextRange sourceRange = field.getNameIdentifier().getTextRange();
		return utils.toRange(openable, sourceRange.getStartOffset(), sourceRange.getLength());
	}

	public static Range toNameRange(PsiClass type, IPsiUtils utils) {
		PsiFile openable = type.getContainingFile();
		TextRange sourceRange = type.getNameIdentifier().getTextRange();
		return utils.toRange(openable, sourceRange.getStartOffset(), sourceRange.getLength());
	}
}
