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

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.qute.psi.QuteMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.qute.psi.QuteMavenProjectName;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link CDIUtils}.
 * 
 * @author Angelo ZERR
 *
 */
public class CDIUtilsTest extends QuteMavenModuleImportingTestCase  {

	@Test
	public void testNamedWithType() {
		String javaType = "MyClass";

		String name = CDIUtils.getSimpleName(javaType, null, PsiClass.class);
		Assert.assertEquals("myClass", name);

		String named = "foo";
		name = CDIUtils.getSimpleName(javaType, named, PsiClass.class);
		Assert.assertEquals("foo", name);
	}
	
	@Test
	public void testNamedWithField() {
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

	@Test
	public void testIsBeanQuarkus3() throws Exception {
		Module javaProject = loadMavenProject(QuteMavenProjectName.quarkus3);
		PsiClass notBean1 = PsiTypeUtils.findType(javaProject, "org.acme.NotBean1");
		// @Decorator annotated class is not a bean
		assertFalse(CDIUtils.isValidBean(notBean1));

		PsiClass notBean2 = PsiTypeUtils.findType(javaProject, "org.acme.NotBean2");
		// @Vetoed annotated class is not a bean
		assertFalse(CDIUtils.isValidBean(notBean2));

		PsiClass bean1 = PsiTypeUtils.findType(javaProject, "org.acme.Bean1");
		// Empty class is a bean
		assertTrue(CDIUtils.isValidBean(bean1));
	}

}
