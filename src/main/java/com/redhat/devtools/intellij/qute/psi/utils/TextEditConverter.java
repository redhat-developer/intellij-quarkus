/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.qute.psi.utils;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Converts an {@link org.eclipse.text.edits.TextEdit} to
 * {@link org.eclipse.lsp4j.TextEdit}
 *
 * @author Gorkem Ercan
 *
 */
public class TextEditConverter {

	private static final Logger LOGGER = Logger.getLogger(TextEditConverter.class.getName());

	private final Document source;
	protected PsiFile compilationUnit;
	protected List<org.eclipse.lsp4j.TextEdit> converted;
	private final IPsiUtils utils;
	private final PsiFile targetCompilationUnit;

	public TextEditConverter(PsiFile unit, PsiFile targetUnit, Document edit, IPsiUtils utils) {
		this.utils = utils;
		this.source = edit;
		this.converted = new ArrayList<>();
		if (unit == null) {
			throw new IllegalArgumentException("Compilation unit can not be null");
		}
		this.compilationUnit = unit;
		this.targetCompilationUnit = targetUnit;
	}

	public List<org.eclipse.lsp4j.TextEdit> convert() {
		if (this.source != null) {
			org.eclipse.lsp4j.TextEdit te = new org.eclipse.lsp4j.TextEdit();
			CodeStyleManager.getInstance(compilationUnit.getProject()).reformatText(targetCompilationUnit, 0,
					source.getTextLength());
			te.setNewText(source.getText());
			Document sourceDocument = PsiDocumentManager.getInstance(compilationUnit.getProject()).getDocument(compilationUnit);
			te.setRange(utils.toRange(sourceDocument, 0, sourceDocument.getTextLength()));
			converted.add(te);
		}
		return converted;
	}

	public TextDocumentEdit convertToTextDocumentEdit(int version) {
		String uri = utils.toUri(compilationUnit);
		VersionedTextDocumentIdentifier identifier = new VersionedTextDocumentIdentifier(version);
		identifier.setUri(uri);
		return new TextDocumentEdit(identifier, this.convert());
	}

}
