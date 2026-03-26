/*******************************************************************************
* Copyright (c) 2025 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.inlayhint;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.AbstractJavaContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintKind;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintSettings;
import org.eclipse.lsp4mp.commons.runtime.EnumConstantsProvider;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.commons.runtime.converter.ConverterValidator;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Java inlayHint context for a given compilation unit.
 *
 * @author Angelo ZERR
 *
 */
public class JavaInlayHintsContext extends AbstractJavaContext {

	private final MicroProfileJavaInlayHintParams params;

	private final List<InlayHint> inlayHints;

	private MicroProfileJavaInlayHintSettings settings;

	public JavaInlayHintsContext(@NotNull String uri,
								 @NotNull PsiFile typeRoot,
								 @NotNull IPsiUtils utils,
								 @NotNull Module module,
								 @NotNull MicroProfileJavaInlayHintParams params,
								 @NotNull List<InlayHint> inlayHints) {
		super(uri, typeRoot, utils, module);
		this.params = params;
		this.inlayHints = inlayHints;
		if (params.getSettings() == null) {
			this.settings = new MicroProfileJavaInlayHintSettings(ExecutionMode.SAFE);
		} else {
			this.settings = params.getSettings();
		}
	}

	public MicroProfileJavaInlayHintSettings getSettings() {
		return settings;
	}

	public MicroProfileJavaInlayHintParams getParams() {
		return params;
	}

	public @NotNull InlayHint addInlayHint(String label, int offset) {
			PsiFile openable = getTypeRoot();
			Range range = getUtils().toRange(openable, offset, 0);
			InlayHint inlayHint = new InlayHint();
			inlayHint.setLabel(label);
			inlayHint.setKind(InlayHintKind.Type);
			inlayHint.setPosition(range.getStart());
			return addInlayHint(inlayHint);
	}

	public @NotNull InlayHint addInlayHint(InlayHint inlayHint) {
		inlayHints.add(inlayHint);
		return inlayHint;
	}

	public void addConverterInlayHint(PsiType fieldBinding, PsiElement node) {
		MicroProfileProjectRuntime projectRuntime = super.getProjectRuntime();
		if (projectRuntime == null) {
			return;
		}
		ExecutionMode preferredMode = getSettings().getMode();
		EnumConstantsProvider.SimpleEnumConstantsProvider provider = new EnumConstantsProvider.SimpleEnumConstantsProvider();
		String fqn = toQualifiedTypeString(fieldBinding, provider);
		ConverterValidator converterValidator = projectRuntime.findConverterValidator(fqn, provider, preferredMode);
		String converter = converterValidator.getConverterSimpleClassName() != null
				? converterValidator.getConverterSimpleClassName()
				: null;
		if (converter != null) {
			addInlayHint(converter + " ", node.getTextOffset());
		}
	}
}
