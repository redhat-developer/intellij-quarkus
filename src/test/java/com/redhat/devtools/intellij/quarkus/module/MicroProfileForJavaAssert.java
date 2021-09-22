/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.module;

import com.redhat.devtools.intellij.quarkus.search.core.PropertiesManagerForJava;
import com.redhat.devtools.intellij.quarkus.search.core.java.diagnostics.IJavaErrorCode;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileDefinition;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDefinitionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaHoverParams;
import org.junit.Assert;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * MicroProfile assert for java files for JUnit tests.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/internal/core/java/MicroProfileForJavaAssert.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/internal/core/java/MicroProfileForJavaAssert.java</a>
 *
 */
public class MicroProfileForJavaAssert {
	// ------------------- Assert for CodeAction

	public static MicroProfileJavaCodeActionParams createCodeActionParams(String uri, Diagnostic d) {
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Range range = d.getRange();
		CodeActionContext context = new CodeActionContext();
		context.setDiagnostics(Arrays.asList(d));
		MicroProfileJavaCodeActionParams codeActionParams = new MicroProfileJavaCodeActionParams(textDocument, range,
				context);
		codeActionParams.setResourceOperationSupported(true);
		return codeActionParams;
	}

	public static void assertJavaCodeAction(MicroProfileJavaCodeActionParams params, IPsiUtils utils,
											CodeAction... expected) {
		List<? extends CodeAction> actual = PropertiesManagerForJava.getInstance().codeAction(params, utils);
		assertCodeActions(actual != null && actual.size() > 0 ? actual : Collections.emptyList(), expected);
	}

	public static void assertCodeActions(List<? extends CodeAction> actual, CodeAction... expected) {
		actual.stream().forEach(ca -> {
			// we don't want to compare title, etc
			ca.setCommand(null);
			ca.setKind(null);
			if (ca.getDiagnostics() != null) {
				ca.getDiagnostics().forEach(d -> {
					d.setSeverity(null);
					d.setMessage("");
					d.setSource(null);
				});
			}
		});

		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			assertEquals("Assert title [" + i + "]", expected[i].getTitle(), actual.get(i).getTitle());
			assertEquals("Assert edit [" + i + "]", expected[i].getEdit(), actual.get(i).getEdit());
		}
	}

	public static CodeAction ca(String uri, String title, Diagnostic d, TextEdit... te) {
		CodeAction codeAction = new CodeAction();
		codeAction.setTitle(title);
		codeAction.setDiagnostics(Arrays.asList(d));

		VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier(uri, 0);

		TextDocumentEdit textDocumentEdit = new TextDocumentEdit(versionedTextDocumentIdentifier, Arrays.asList(te));
		WorkspaceEdit workspaceEdit = new WorkspaceEdit(Arrays.asList(Either.forLeft(textDocumentEdit)));
		workspaceEdit.setChanges(Collections.emptyMap());
		codeAction.setEdit(workspaceEdit);
		return codeAction;
	}

	public static TextEdit te(int startLine, int startCharacter, int endLine, int endCharacter, String newText) {
		TextEdit textEdit = new TextEdit();
		textEdit.setNewText(newText);
		textEdit.setRange(r(startLine, startCharacter, endLine, endCharacter));
		return textEdit;
	}

	// Assert for diagnostics

	public static Diagnostic d(int line, int startCharacter, int endCharacter, String message,
							   DiagnosticSeverity severity, final String source, IJavaErrorCode code) {
		return d(line, startCharacter, line, endCharacter, message, severity, source, code);
	}

	public static Diagnostic d(int startLine, int startCharacter, int endLine, int endCharacter, String message,
							   DiagnosticSeverity severity, final String source, IJavaErrorCode code) {
		// Diagnostic on 1 line
		return new Diagnostic(r(startLine, startCharacter, endLine, endCharacter), message, severity, source,
				code != null ? code.getCode() : null);
	}

	public static Range r(int line, int startCharacter, int endCharacter) {
		return r(line, startCharacter, line, endCharacter);
	}

	public static Range r(int startLine, int startCharacter, int endLine, int endCharacter) {
		return new Range(p(startLine, startCharacter), p(endLine, endCharacter));
	}

	public static Position p(int line, int character) {
		return new Position(line, character);
	}

	public static void assertJavaDiagnostics(MicroProfileJavaDiagnosticsParams params, IPsiUtils utils,
											 Diagnostic... expected) {
		List<PublishDiagnosticsParams> actual = PropertiesManagerForJava.getInstance().diagnostics(params, utils);
		assertDiagnostics(
				actual != null && actual.size() > 0 ? actual.get(0).getDiagnostics() : Collections.emptyList(),
				expected);
	}

	public static void assertDiagnostics(List<Diagnostic> actual, Diagnostic... expected) {
		assertDiagnostics(actual, Arrays.asList(expected), false);
	}

	public static void assertDiagnostics(List<Diagnostic> actual, List<Diagnostic> expected, boolean filter) {
		List<Diagnostic> received = actual;
		final boolean filterMessage;
		if (expected != null && !expected.isEmpty()
				&& (expected.get(0).getMessage() == null || expected.get(0).getMessage().isEmpty())) {
			filterMessage = true;
		} else {
			filterMessage = false;
		}
		if (filter) {
			received = actual.stream().map(d -> {
				Diagnostic simpler = new Diagnostic(d.getRange(), "");
				simpler.setCode(d.getCode());
				if (filterMessage) {
					simpler.setMessage(d.getMessage());
				}
				return simpler;
			}).collect(Collectors.toList());
		}
		assertEquals("Unexpected diagnostics:\n" + actual, expected, received);
	}

	// Assert for Hover

	public static void assertJavaHover(Position hoverPosition, String javaFileUri, IPsiUtils utils, Hover expected) {
		MicroProfileJavaHoverParams params = new MicroProfileJavaHoverParams();
		params.setDocumentFormat(DocumentFormat.Markdown);
		params.setPosition(hoverPosition);
		params.setUri(javaFileUri);
		params.setSurroundEqualsWithSpaces(true);
		assertJavaHover(params, utils, expected);
	}

	public static void assertJavaHover(MicroProfileJavaHoverParams params, IPsiUtils utils, Hover expected) {
		Hover actual = PropertiesManagerForJava.getInstance().hover(params, utils);
		assertHover(expected, actual);
	}

	public static void assertHover(Hover expected, Hover actual) {
		if (expected == null || actual == null) {
			assertEquals(expected, actual);
		} else {
			assertEquals(expected.getContents().getRight(), actual.getContents().getRight());
			assertEquals(expected.getRange(), actual.getRange());
		}
	}

	public static Hover h(String hoverContent, int startLine, int startCharacter, int endLine, int endCharacter) {
		Range range = r(startLine, startCharacter, endLine, endCharacter);
		Hover hover = new Hover();
		hover.setContents(Either.forRight(new MarkupContent(MarkupKind.MARKDOWN, hoverContent)));
		hover.setRange(range);
		return hover;
	}

	public static Hover h(String hoverContent, int line, int startCharacter, int endCharacter) {
		return h(hoverContent, line, startCharacter, line, endCharacter);
	}

	// Assert for Definition

	public static void assertJavaDefinitions(Position position, String javaFileUri, IPsiUtils utils,
											 MicroProfileDefinition... expected) {
		MicroProfileJavaDefinitionParams params = new MicroProfileJavaDefinitionParams();
		params.setPosition(position);
		params.setUri(javaFileUri);
		List<MicroProfileDefinition> actual = PropertiesManagerForJava.getInstance().definition(params, utils);
		assertDefinitions(actual, expected);
	}

	public static void assertDefinitions(List<MicroProfileDefinition> actual, MicroProfileDefinition... expected) {
		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			assertEquals("Assert selectPropertyName [" + i + "]", expected[i].getSelectPropertyName(),
					actual.get(i).getSelectPropertyName());
			assertEquals("Assert location [" + i + "]", expected[i].getLocation(), actual.get(i).getLocation());
		}
	}

	public static MicroProfileDefinition def(Range originSelectionRange, String targetUri, Range targetRange) {
		return def(originSelectionRange, targetUri, targetRange, null);
	}

	public static MicroProfileDefinition def(Range originSelectionRange, String targetUri, String selectPropertyName) {
		return def(originSelectionRange, targetUri, null, selectPropertyName);
	}

	private static MicroProfileDefinition def(Range originSelectionRange, String targetUri, Range targetRange,
											  String selectPropertyName) {
		MicroProfileDefinition definition = new MicroProfileDefinition();
		LocationLink location = new LocationLink();
		location.setOriginSelectionRange(originSelectionRange);
		location.setTargetUri(targetUri);
		if (targetRange != null) {
			location.setTargetRange(targetRange);
			location.setTargetSelectionRange(targetRange);
		}
		definition.setLocation(location);
		definition.setSelectPropertyName(selectPropertyName);
		return definition;
	}

	public static String fixURI(URI uri) {
		String uriString = uri.toString();
		return uriString.replaceFirst("file:/([^/])", "file:///$1");
	}
}
