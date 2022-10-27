/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copied from /org.eclipse.jdt.ui/src/org/eclipse/jdt/internal/ui/text/correction/proposals/ImplementInterfaceProposal.java
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.eclipse.lsp4j.CodeActionKind;

import java.text.MessageFormat;

public class ImplementInterfaceProposal extends ASTRewriteCorrectionProposal {

	private static final String TITLE_MESSAGE = "Let ''{0}'' implement ''{1}''";

	private PsiClass fBinding;
	private PsiFile fAstRoot;
	private String interfaceType;

	public ImplementInterfaceProposal(PsiFile targetCU, PsiClass binding, PsiFile astRoot,
									  String interfaceType, int relevance, PsiFile sourceCU) {
		super("", CodeActionKind.QuickFix, targetCU, relevance, sourceCU); //$NON-NLS-1$

		//Assert.isTrue(binding != null && Bindings.isDeclarationBinding(binding));

		fBinding = binding;
		fAstRoot = astRoot;
		this.interfaceType = interfaceType;

		String[] args = { binding.getName(),
				interfaceType };
		setDisplayName(MessageFormat.format(TITLE_MESSAGE, (Object[]) args));
	}

	@Override
	public void performUpdate() {
		PsiClass interfaceClass = JavaPsiFacade.getInstance(fBinding.getProject()).
				findClass(interfaceType, GlobalSearchScope.allScope(fBinding.getProject()));
		if (interfaceClass != null) {
			fBinding.getImplementsList().add(PsiElementFactory.getInstance(fBinding.getProject()).
					createClassReferenceElement(interfaceClass));
		}
	}
}
