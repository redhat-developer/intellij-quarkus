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
 * Tests for {@link QutePsiFile#findElementAt(int)} to verify it returns
 * the correct Qute PSI elements (QuteASTPart) instead of template language elements.
 */
public class QutePsiFileFindElementAtTest extends BasePlatformTestCase {

    public void testFindElementAt_SimpleObjectPart() {
        // Test with simple expression: {item}
        PsiFile file = myFixture.configureByText(QuteFileType.QUTE, "{item}");

        // Find element at 'i' in {item}
        PsiElement element = file.findElementAt(1);

        assertNotNull("Element should not be null", element);

        // Verify it's a QuteASTObjectPart or its child
        boolean isQuteElement = element instanceof QuteASTObjectPart
                || element.getParent() instanceof QuteASTObjectPart;
        assertTrue("Element should be QuteASTObjectPart or its child, but was: " + element.getClass().getName(),
                isQuteElement);
    }

    public void testFindElementAt_PropertyPart() {
        // Test with property expression: {item.name}
        PsiFile file = myFixture.configureByText(QuteFileType.QUTE, "{item.name}");

        // Find element at 'n' in {item.name}
        PsiElement element = file.findElementAt(6);

        assertNotNull("Element should not be null", element);

        // Verify it's a QuteASTPropertyPart or its child
        boolean isQuteElement = element instanceof QuteASTPropertyPart
                || element.getParent() instanceof QuteASTPropertyPart;
        assertTrue("Element should be QuteASTPropertyPart or its child, but was: " + element.getClass().getName(),
                isQuteElement);
    }

    public void testFindElementAt_NamespacedObjectPart() {
        // Test with namespaced expression: {uri:item}
        PsiFile file = myFixture.configureByText(QuteFileType.QUTE, "{uri:item}");

        // Find element at 'i' in {uri:item}
        PsiElement element = file.findElementAt(5);

        assertNotNull("Element should not be null", element);

        // Verify it's a QuteASTObjectPart or its child
        boolean isQuteElement = element instanceof QuteASTObjectPart
                || element.getParent() instanceof QuteASTObjectPart;
        assertTrue("Element should be QuteASTObjectPart or its child, but was: " + element.getClass().getName(),
                isQuteElement);
    }

    public void testFindElementAt_ComplexExpression() {
        // Test with complex expression: {uri:item.name}
        PsiFile file = myFixture.configureByText(QuteFileType.QUTE, "{uri:item.name}");

        // Test at object part
        PsiElement objectElement = file.findElementAt(5); // 'i' in item
        assertNotNull("Object element should not be null", objectElement);

        // Test at property part
        PsiElement propertyElement = file.findElementAt(10); // 'n' in name
        assertNotNull("Property element should not be null", propertyElement);
    }

    public void testFindElementAt_HTMLContent() {
        // Test with HTML content outside Qute expressions
        PsiFile file = myFixture.configureByText(QuteFileType.QUTE, "<h1>Hello {name}!</h1>");

        // Find element at 'H' in <h1>
        PsiElement htmlElement = file.findElementAt(1);
        assertNotNull("HTML element should not be null", htmlElement);

        // Find element at 'n' in {name}
        PsiElement quteElement = file.findElementAt(13);
        assertNotNull("Qute element should not be null", quteElement);
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources";
    }
}
