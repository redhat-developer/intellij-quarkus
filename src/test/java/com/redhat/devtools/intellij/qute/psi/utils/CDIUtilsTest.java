/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.utils;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link CDIUtils}.
 * 
 * @author Angelo ZERR
 *
 */
public class CDIUtilsTest {

	@Test
	public void namedWithType() {
		String javaType = "MyClass";

		String name = CDIUtils.getSimpleName(javaType, null, PsiClass.class);
		Assert.assertEquals("myClass", name);

		String named = "foo";
		name = CDIUtils.getSimpleName(javaType, named, PsiClass.class);
		Assert.assertEquals("foo", name);
	}
	
	@Test
	public void namedWithField() {
		String javaType = "MyField";

		String name = CDIUtils.getSimpleName(javaType, null, PsiField.class);
		Assert.assertEquals("MyField", name);

		String named = "foo";
		name = CDIUtils.getSimpleName(javaType, named, PsiField.class);
		Assert.assertEquals("foo", name);
	}
	
	@Test
	public void namedWithMethod() {
		String javaType = "MyMethod";

		String name = CDIUtils.getSimpleName(javaType, null, PsiMethod.class);
		Assert.assertEquals("MyMethod", name);

		String named = "foo";
		name = CDIUtils.getSimpleName(javaType, named, PsiMethod.class);
		Assert.assertEquals("foo", name);
	}
	
	@Test
	public void namedWithGetterMethod() {
		String javaType = "getMethod";

		String name = CDIUtils.getSimpleName(javaType, null, PsiMethod.class, () -> true);
		Assert.assertEquals("method", name);

		String named = "foo";
		name = CDIUtils.getSimpleName(javaType, named, PsiMethod.class, () -> true);
		Assert.assertEquals("foo", name);
	}
}
