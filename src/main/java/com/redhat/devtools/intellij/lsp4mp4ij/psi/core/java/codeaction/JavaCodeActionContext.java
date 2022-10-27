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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.AbstractJavaContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.java.ChangeUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;

import java.util.Arrays;

/**
 * Java codeAction context for a given compilation unit.
 *
 * @author Angelo ZERR
 *
 */
public class JavaCodeActionContext extends AbstractJavaContext implements IInvocationContext {

	private final int selectionOffset;
	private final int selectionLength;

	private final MicroProfileJavaCodeActionParams params;
	private JavaCodeActionContext source;

	public JavaCodeActionContext(PsiFile typeRoot, int selectionOffset, int selectionLength, IPsiUtils utils,
								 Module module, MicroProfileJavaCodeActionParams params, JavaCodeActionContext source) {
		super(params.getUri(), typeRoot, utils, module);
		this.selectionOffset = selectionOffset;
		this.selectionLength = selectionLength;
		this.params = params;
		this.source = source;
	}

	public JavaCodeActionContext(PsiFile typeRoot, int selectionOffset, int selectionLength, IPsiUtils utils,
								 Module module, MicroProfileJavaCodeActionParams params) {
		this(typeRoot, selectionOffset, selectionLength,utils, module, params, null);
		this.source = this;
	}

	/**
	 * Return a copy of the context with its own in memory compilation unit
	 *
	 * @return the new context
	 */
	public JavaCodeActionContext oopy() {
		return new JavaCodeActionContext(getTypeRoot().getViewProvider().clone().getPsi(getTypeRoot().getLanguage()), selectionOffset,
				selectionLength, getUtils(), getJavaProject(), params, this.source);
	}

	public MicroProfileJavaCodeActionParams getParams() {
		return params;
	}

	@Override
	public PsiFile getCompilationUnit() {
		return getTypeRoot();
	}

	/**
	 * Returns the length.
	 *
	 * @return int
	 */
	@Override
	public int getSelectionLength() {
		return selectionLength;
	}

	/**
	 * Returns the offset.
	 *
	 * @return int
	 */
	@Override
	public int getSelectionOffset() {
		return selectionOffset;
	}

	@Override
	public PsiElement getCoveringNode() {
		return PsiUtil.getElementInclusiveRange(getASTRoot(), TextRange.from(getSelectionOffset(), getSelectionLength()));
	}

	@Override
	public PsiElement getCoveredNode() {
		return getASTRoot().findElementAt(getSelectionOffset());
	}

	public JavaCodeActionContext getSource() {
		return source;
	}

	public CodeAction convertToCodeAction(ChangeCorrectionProposal proposal, Diagnostic... diagnostics) {
		String name = proposal.getName();
		WorkspaceEdit edit = ChangeUtil.convertToWorkspaceEdit(proposal.getChange(), getUri(), getUtils(),
				params.isResourceOperationSupported());
		if (!ChangeUtil.hasChanges(edit)) {
			return null;
		}
		ExtendedCodeAction codeAction = new ExtendedCodeAction(name);
		codeAction.setRelevance(proposal.getRelevance());
		codeAction.setKind(proposal.getKind());
		codeAction.setEdit(edit);
		codeAction.setDiagnostics(Arrays.asList(diagnostics));
		return codeAction;
	}
}
