/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.lang.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.redhat.devtools.intellij.qute.lang.QuteASTObjectPart;
import com.redhat.devtools.intellij.qute.lang.QuteASTPropertyPart;
import com.redhat.devtools.intellij.qute.lang.QuteFileType;

/**
 * Tests for {@link QutePsiFile#findElementAt(int)} with alternative expression syntax.
 * <p>
 * Alternative syntax uses {=...} instead of {...} for Qute expressions.
 * <p>
 * These tests use {@link QuteLexer#FORCE_ALT_EXPR_SYNTAX} UserData key to enable
 * alt-expr-syntax in the test environment.
 */
public class QutePsiFileFindElementAtAltSyntaxTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Enable alt-expr-syntax via UserData (workaround for testing)
        getProject().putUserData(QuteLexer.FORCE_ALT_EXPR_SYNTAX, true);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            // Clean up UserData
            getProject().putUserData(QuteLexer.FORCE_ALT_EXPR_SYNTAX, null);
        } finally {
            super.tearDown();
        }
    }

    public void testFindElementAt_AlternativeSyntax_SimpleObjectPart() {
        // Test with alternative syntax: {=item}
        PsiFile file = myFixture.configureByText(QuteFileType.QUTE, "{=item}");

        System.out.println("\n=== Analyzing {=item} (alt syntax) ===");
        System.out.println("File class: " + file.getClass().getName());
        System.out.println("File language: " + file.getLanguage());
        System.out.println("File virtual file: " + file.getVirtualFile());

        // Find element at 'i' in {=item} (offset 2 because of '=' at offset 1)
        PsiElement element = file.findElementAt(2);

        assertNotNull("Element should not be null", element);

        // Print debug info
        printElementDebugInfo("Alt syntax object (offset 2)", element);

        // Verify it's a QuteASTObjectPart or its child
        boolean isQuteElement = element instanceof QuteASTObjectPart
                || element.getParent() instanceof QuteASTObjectPart;

        if (!isQuteElement) {
            System.out.println("\n⚠️  Alt-expr-syntax not detected:");
            System.out.println("   Expected: QuteASTObjectPart");
            System.out.println("   Actual: " + element.getClass().getName());
            System.out.println("   This means application.properties was not read by PsiMicroProfileProjectManager");
        }

        assertTrue("Element should be QuteASTObjectPart or its child, but was: " + element.getClass().getName(),
                isQuteElement);
    }

    public void testFindElementAt_AlternativeSyntax_PropertyPart() {
        PsiFile file = myFixture.configureByText(QuteFileType.QUTE, "{=item.name}");

        PsiElement element = file.findElementAt(7);
        assertNotNull("Element should not be null", element);

        printElementDebugInfo("Alt syntax property (offset 7)", element);

        // Verify it's a QuteASTPropertyPart or its child
        boolean isQuteElement = element instanceof QuteASTPropertyPart
                || element.getParent() instanceof QuteASTPropertyPart;
        assertTrue("Element should be QuteASTPropertyPart or its child, but was: " + element.getClass().getName(),
                isQuteElement);
    }

    public void testFindElementAt_AlternativeSyntax_NamespacedObjectPart() {
        PsiFile file = myFixture.configureByText(QuteFileType.QUTE, "{=uri:item}");

        PsiElement element = file.findElementAt(6);
        assertNotNull("Element should not be null", element);

        printElementDebugInfo("Alt syntax namespace (offset 6)", element);

        // Verify it's a QuteASTObjectPart or its child
        boolean isQuteElement = element instanceof QuteASTObjectPart
                || element.getParent() instanceof QuteASTObjectPart;
        assertTrue("Element should be QuteASTObjectPart or its child, but was: " + element.getClass().getName(),
                isQuteElement);
    }

    public void testFindElementAt_AlternativeSyntax_WithLetDeclaration() {
        // Test with let declaration followed by alt-syntax usage
        // This is a production bug: can't find 'foo' in {=foo.blank.is(true)}
        String content = "{@java.lang.String foo}\n{=foo.blank.is(true)}";
        PsiFile file = myFixture.configureByText(QuteFileType.QUTE, content);

        System.out.println("\n=== Analyzing alt-syntax with let declaration ===");
        System.out.println("Content:\n" + content);

        // Find 'foo' in {=foo.blank.is(true)}
        // Position: "{@java.lang.String foo}\n{=foo.blank.is(true)}"
        //            012345678901234567890123 24 252627282930...
        int fooOffset = content.indexOf("{=foo") + 2; // Position of 'f' in {=foo

        System.out.println("Looking for 'foo' at offset: " + fooOffset);
        System.out.println("Character at offset: '" + content.charAt(fooOffset) + "'");

        PsiElement element = file.findElementAt(fooOffset);
        assertNotNull("Element should not be null", element);

        printElementDebugInfo("Alt syntax 'foo' after let declaration (offset " + fooOffset + ")", element);

        // Verify it's a QuteASTObjectPart (foo) or its child
        boolean isQuteElement = element instanceof QuteASTObjectPart
                || element.getParent() instanceof QuteASTObjectPart;

        if (!isQuteElement) {
            System.out.println("\n❌ BUG REPRODUCED:");
            System.out.println("   Expected: QuteASTObjectPart for 'foo'");
            System.out.println("   Actual: " + element.getClass().getName());
            System.out.println("   This is the production bug we're trying to fix!");
        }

        assertTrue("Element should be QuteASTObjectPart for 'foo', but was: " + element.getClass().getName(),
                isQuteElement);
    }

    private void printElementDebugInfo(String label, PsiElement element) {
        System.out.println("\n=== " + label + " ===");
        System.out.println("Element: " + element.getClass().getName() + " = " + element);
        System.out.println("  Text: '" + element.getText() + "'");
        System.out.println("  TextRange: " + element.getTextRange());
        if (element.getNode() != null) {
            System.out.println("  Node type: " + element.getNode().getElementType());
        } else {
            System.out.println("  Node type: null (no AST node)");
        }

        PsiElement parent = element.getParent();
        int level = 1;
        while (parent != null && level <= 5) {
            String nodeType = parent.getNode() != null
                    ? parent.getNode().getElementType().toString()
                    : "null";
            System.out.println("  Parent[" + level + "]: " + parent.getClass().getName()
                    + " - " + nodeType
                    + " - Range: " + parent.getTextRange());
            parent = parent.getParent();
            level++;
        }
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources";
    }
}
