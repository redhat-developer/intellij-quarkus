/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.quarkus.mp4ij.psi.internal.faulttolerance.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.quarkus.mp4ij.psi.core.java.completion.IJavaCompletionParticipant;
import com.redhat.devtools.intellij.quarkus.mp4ij.psi.core.java.completion.JavaCompletionContext;
import com.redhat.devtools.intellij.quarkus.search.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileFaultToleranceConstants.FALLBACK_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileFaultToleranceConstants.FALLBACK_METHOD_FALLBACK_ANNOTATION_MEMBER;

/**
 * Completion for <code>fallbackMethod</code>
 *
 * @author datho7561
 */
public class MicroProfileFaultToleranceCompletionParticipant implements IJavaCompletionParticipant {

	private static final Pattern FALLBACK_METHOD_KEY_VALUE_PATTERN = Pattern
			.compile(FALLBACK_METHOD_FALLBACK_ANNOTATION_MEMBER + "\\s*=\\s*\"([^\"]*)\"");

	private static final String FALLBACK_ANNOTATION_SHORT_NAME = FALLBACK_ANNOTATION
			.substring(FALLBACK_ANNOTATION.lastIndexOf('.') + 1);

	@Override
	public boolean isAdaptedForCompletion(JavaCompletionContext context) {
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, FALLBACK_ANNOTATION) != null;
	}

	@Override
	public List<? extends CompletionItem> collectCompletionItems(JavaCompletionContext context) {
		PsiMethod method = getMethod(context.getTypeRoot(), context.getOffset());
		PsiAnnotation fallbackAnnotation = getFallbackAnnotation(method, context.getOffset());
		if (fallbackAnnotation == null) {
			return null;
		}
		Range range = getCompletionReplaceRange(fallbackAnnotation, context.getUtils(), context.getOffset());
		if (range == null) {
			return null;
		}
		List<CompletionItem> completionItems = new ArrayList<>();
		for (PsiMethod m : method.getContainingClass().getMethods()) {
			completionItems.add(makeMethodCompletionItem(m.getName(), range));
		}
		return completionItems;
	}

	/**
	 * Returns the <code>@Fallback</code> annotation as an IAnnotation or null if
	 * the offset is not in a <code>@Fallback</code> annotation
	 *
	 * @param method the type root of the class that is being checked for an
	 *                 annotation
	 * @param offset   the offset at which completion is triggered
	 * @return the <code>@Fallback</code> annotation as an IAnnotation or null if
	 *         the offset is not in a <code>@Fallback</code> annotation
	 */
	private static PsiAnnotation getFallbackAnnotation(PsiMethod method, int offset) {
		if (method == null) return null;
		PsiAnnotation annotation = AnnotationUtils.getAnnotation(method, FALLBACK_ANNOTATION);
		return annotation;
	}

	@Nullable
	private static PsiMethod getMethod(PsiFile typeRoot, int offset) {
		PsiMethod element = PsiTreeUtil.getParentOfType(typeRoot.findElementAt(offset), PsiMethod.class);
		if (!(element instanceof PsiMethod)) {
			return null;
		}
		return element;
	}

	/**
	 * Returns the range that should be replaced for completion, or null otherwise
	 *
	 * @param fallbackAnnotation the fallback annotation
	 * @param utils              the IJDTUtils
	 * @param triggerOffset      the offset in the document where completion was
	 *                           triggered
	 * @return the range that should be replaced for completion, or null otherwise
	 */
	private static Range getCompletionReplaceRange(PsiAnnotation fallbackAnnotation, IPsiUtils utils, int triggerOffset) {
		TextRange range = fallbackAnnotation.getTextRange();
		if (range == null || range.getStartOffset() == (-1)) {
			return null;
		}
		int annotationStart = range.getStartOffset();
		String annotationSrc = fallbackAnnotation.getText();
		Matcher m = FALLBACK_METHOD_KEY_VALUE_PATTERN.matcher(annotationSrc);
		if (!m.find()) {
			return null;
		}
		if (m.start(1) == -1 || m.end(1) == -1) {
			return null;
		}
		int start = m.start(1) + annotationStart;
		int length = m.end(1) - m.start(1);
		if (triggerOffset < start || start + length < triggerOffset) {
			return null;
		}
		return utils.toRange(fallbackAnnotation, start, length);
	}

	/**
	 * Returns the method completion item given the name of the method
	 *
	 * @param methodName   the name of the method
	 * @param replaceRange the range in the document that should be replaced with
	 *                     the method name
	 * @return the method completion item given the name of the method
	 */
	private static CompletionItem makeMethodCompletionItem(String methodName, Range replaceRange) {
		CompletionItem completionItem = new CompletionItem();
		TextEdit textEdit = new TextEdit(replaceRange, methodName);
		completionItem.setTextEdit(Either.forLeft(textEdit));
		completionItem.setKind(CompletionItemKind.Method);
		completionItem.setLabel(methodName + "()");
		return completionItem;
	}

}
