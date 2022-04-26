/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.template;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.qute.psi.QuteMavenProjectName;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import org.junit.Assert;
import org.junit.Test;

import com.redhat.qute.commons.JavaTypeInfo;
import com.redhat.qute.commons.JavaTypeKind;
import com.redhat.qute.commons.QuteJavaTypesParams;

/**
 * Tests for
 * {@link QuteSupportForTemplate#getJavaTypes(QuteJavaTypesParams, com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils, com.intellij.openapi.progress.ProgressIndicator)}
 *
 * @author Angelo ZERR
 *
 */
public class TemplateGetJavaTypeTest extends MavenModuleImportingTestCase {
	private Module module;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		module = createMavenModule(QuteMavenProjectName.qute_quickstart, new File("projects/qute/projects/maven/" + QuteMavenProjectName.qute_quickstart));
	}

	@Test
	public void testpackages() throws Exception {

		QuteJavaTypesParams params = new QuteJavaTypesParams("java.", "qute-quickstart");
		List<JavaTypeInfo> actual = QuteSupportForTemplate.getInstance().getJavaTypes(params, PsiUtilsLSImpl.getInstance(myProject),
				new EmptyProgressIndicator());

		assertJavaTypes(actual, //
				t("java.util", JavaTypeKind.Package), //
				t("java.lang", JavaTypeKind.Package));
	}

	@Test
	public void testlist() throws Exception {

		QuteJavaTypesParams params = new QuteJavaTypesParams("List", "qute-quickstart");
		List<JavaTypeInfo> actual = QuteSupportForTemplate.getInstance().getJavaTypes(params, PsiUtilsLSImpl.getInstance(myProject),
				new EmptyProgressIndicator());

		assertJavaTypes(actual, //
				t("java.util.List<E>", JavaTypeKind.Interface));
	}

	@Test
	public void testitem() throws Exception {

		QuteJavaTypesParams params = new QuteJavaTypesParams("Item", "qute-quickstart");
		List<JavaTypeInfo> actual = QuteSupportForTemplate.getInstance().getJavaTypes(params, PsiUtilsLSImpl.getInstance(myProject),
				new EmptyProgressIndicator());

		assertJavaTypes(actual, //
				t("org.acme.qute.Item", JavaTypeKind.Class), //
				t("org.acme.qute.ItemResource", JavaTypeKind.Class));
	}

	@Test
	public void testnested() throws Exception {

		QuteJavaTypesParams params = new QuteJavaTypesParams("org.acme.qute.NestedClass.",
				"qute-quickstart");
		List<JavaTypeInfo> actual = QuteSupportForTemplate.getInstance().getJavaTypes(params, PsiUtilsLSImpl.getInstance(myProject),
				new EmptyProgressIndicator());

		assertJavaTypes(actual, //
				t("org.acme.qute.NestedClass.Foo", JavaTypeKind.Class), //
				t("org.acme.qute.NestedClass.Bar", JavaTypeKind.Class));
	}

	public static JavaTypeInfo t(String typeName, JavaTypeKind kind) {
		JavaTypeInfo javaType = new JavaTypeInfo();
		javaType.setSignature(typeName);
		javaType.setKind(kind);
		return javaType;
	}

	/**
	 * Assert Java types.
	 *
	 * @param actual   the actual java types
	 * @param expected the expected Java types.
	 */
	public static void assertJavaTypes(List<JavaTypeInfo> actual, JavaTypeInfo... expected) {
		assertJavaTypes(actual, null, expected);
	}

	/**
	 * Assert Java types.
	 *
	 * @param actual        the actual java types.
	 * @param expectedCount Java types expected count.
	 * @param expected      the expected Java types.
	 */
	public static void assertJavaTypes(List<JavaTypeInfo> actual, Integer expectedCount, JavaTypeInfo... expected) {
		if (expectedCount != null) {
			Assert.assertEquals(expectedCount.intValue(), actual.size());
		}
		for (JavaTypeInfo javaType : expected) {
			assertJavaType(actual, javaType);
		}
	}

	/**
	 * Assert Java type.
	 *
	 * @param actualTypes the actual java types.
	 * @param expected    the expected Java type.
	 */
	private static void assertJavaType(List<JavaTypeInfo> actualTypes, JavaTypeInfo expected) {
		List<JavaTypeInfo> matches = actualTypes.stream().filter(completion -> {
			return expected.getSignature().equals(completion.getSignature());
		}).collect(Collectors.toList());

		Assert.assertEquals(expected.getSignature() + " should only exist once: Actual: " //
				+ actualTypes.stream().map(c -> c.getSignature()).collect(Collectors.joining(",")), //
				1, matches.size());

		JavaTypeInfo actual = matches.get(0);
		Assert.assertEquals("Test 'type name' for '" + expected.getSignature() + "'", expected.getSignature(),
				actual.getSignature());
		Assert.assertEquals("Test 'kind' for '" + expected.getSignature() + "'", expected.getJavaElementKind(),
				actual.getJavaElementKind());
	}

}
