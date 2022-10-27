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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.java;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Converts an {@link DocumentEvent} to
 * {@link org.eclipse.lsp4j.TextEdit}
 *
 * @author Gorkem Ercan
 *
 */
public class TextEditConverter {

	private static final Logger LOGGER = Logger.getLogger(TextEditConverter.class.getName());

	private final Change source;
	protected PsiFile compilationUnit;
	protected List<org.eclipse.lsp4j.TextEdit> converted;

	private final String uri;

	private final IPsiUtils utils;

	public TextEditConverter(PsiFile unit, Change edit, String uri, IPsiUtils utils) {
		this.source = edit;
		this.converted = new ArrayList<>();
		if (unit == null) {
			throw new IllegalArgumentException("Compilation unit can not be null");
		}
		this.compilationUnit = unit;
		this.uri = uri;
		this.utils = utils;
	}

	public List<org.eclipse.lsp4j.TextEdit> convert() {
		org.eclipse.lsp4j.TextEdit te = new org.eclipse.lsp4j.TextEdit();
		te.setNewText(source.getTargetDocument().getText());
		te.setRange(utils.toRange(source.getSourceDocument(), 0, source.getSourceDocument().getTextLength()));
		converted.add(te);
		return converted;
	}

	public TextDocumentEdit convertToTextDocumentEdit(int version) {
		VersionedTextDocumentIdentifier identifier = new VersionedTextDocumentIdentifier(version);
		identifier.setUri(uri);
		return new TextDocumentEdit(identifier, this.convert());
	}
}
