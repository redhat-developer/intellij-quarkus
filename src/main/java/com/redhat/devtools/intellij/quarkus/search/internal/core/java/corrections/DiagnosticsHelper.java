/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.internal.core.java.corrections;

import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;

/**
 * Helper methods for {@link Diagnostic}
 *
 * @author Gorkem Ercan
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/java/corrections/DiagnosticsHelper.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/java/corrections/DiagnosticsHelper.java</a>
 *
 */
public class DiagnosticsHelper {

	/**
	 * Gets the end offset for the diagnostic.
	 *
	 * @param unit
	 * @param range
	 * @return starting offset or negative value if can not be determined
	 */
	public static int getEndOffset(PsiFile unit, Range range, IPsiUtils utils){
			return utils.toOffset(unit, range.getEnd().getLine(), range.getEnd().getCharacter());
	}

	/**
	 * Gets the start offset for the diagnostic.
	 *
	 * @param unit
	 * @param range
	 * @return starting offset or negative value if can not be determined
	 */
	public static int getStartOffset(PsiFile unit, Range range, IPsiUtils utils){
			return utils.toOffset(unit, range.getStart().getLine(), range.getStart().getCharacter());
	}
	/**
	 * Returns the length of the diagnostic
	 *
	 * @param unit
	 * @param diagnostic
	 * @return length of the diagnostics range.
	 */
	public static int getLength(PsiFile unit, Range range, IPsiUtils utils) {
		int start = DiagnosticsHelper.getStartOffset(unit, range, utils);
		int end = DiagnosticsHelper.getEndOffset(unit, range, utils);
		return end-start;
	}
}
