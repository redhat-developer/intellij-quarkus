/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.CodeAction;

/**
 * Extends LSP CodeAction to store relevance information used to sort the code
 * actions.
 *
 * @author Angelo ZERR
 *
 */
public class ExtendedCodeAction extends CodeAction {

	private static class CodeActionComparator implements Comparator<CodeAction> {

		@Override
		public int compare(CodeAction ca1, CodeAction ca2) {
			String k1 = ca1.getKind();
			String k2 = ca2.getKind();
			if (!StringUtils.isBlank(k1) && !StringUtils.isBlank(k2) && !k1.equals(k2)) {
				return k1.compareTo(k2);
			}

			if (ca1 instanceof ExtendedCodeAction && ca2 instanceof ExtendedCodeAction) {
				int r1 = ((ExtendedCodeAction) ca1).getRelevance();
				int r2 = ((ExtendedCodeAction) ca2).getRelevance();
				int relevanceDif = r2 - r1;
				if (relevanceDif != 0) {
					return relevanceDif;
				}
			}
			return ca1.getTitle().compareToIgnoreCase(ca2.getTitle());
		}
	}

	private static final CodeActionComparator CODE_ACTION_COMPARATOR = new CodeActionComparator();

	private transient int relevance;

	public ExtendedCodeAction(String name) {
		super(name);
	}

	/**
	 * Returns the relevance.
	 *
	 * @return the relevance.
	 */
	public int getRelevance() {
		return relevance;
	}

	/**
	 * Sets the relevance
	 *
	 * @param relevance the relevance
	 */
	public void setRelevance(int relevance) {
		this.relevance = relevance;
	}

	/**
	 * Sort the given code actions list by using relevance information.
	 *
	 * @param codeActions code actions to sort
	 */
	public static void sort(List<CodeAction> codeActions) {
		Collections.sort(codeActions, CODE_ACTION_COMPARATOR);
	}
}
