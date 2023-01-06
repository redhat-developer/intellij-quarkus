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
import java.util.Optional;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.qute.psi.QuteMavenProjectName;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.qute.commons.JavaTypeKind;
import org.junit.Assert;
import org.junit.Test;

import com.redhat.qute.commons.InvalidMethodReason;
import com.redhat.qute.commons.JavaMethodInfo;
import com.redhat.qute.commons.QuteResolvedJavaTypeParams;
import com.redhat.qute.commons.ResolvedJavaTypeInfo;

/**
 * Tests for
 * {@link QuteSupportForTemplate#getResolvedJavaType(QuteResolvedJavaTypeParams, com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils, com.intellij.openapi.progress.ProgressIndicator)}
 *
 * @author Angelo ZERR
 *
 */
public class TemplateGetResolvedJavaTypeTest extends MavenModuleImportingTestCase {
	private Module module;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		module = createMavenModule(new File("projects/qute/projects/maven/qute-quickstart"));
	}

	@Test
	public void testiterable() throws Exception {

		QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("java.lang.Iterable",
				"qute-quickstart");
		ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertEquals("java.lang.Iterable<T>", result.getSignature());
		Assert.assertTrue(result.isIterable());
		Assert.assertEquals("java.lang.Iterable", result.getIterableType());
		Assert.assertEquals("java.lang.Object", result.getIterableOf());
		Assert.assertEquals(JavaTypeKind.Interface, result.getJavaTypeKind());

		params = new QuteResolvedJavaTypeParams("Iterable", "qute-quickstart");
		result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertEquals("java.lang.Iterable<T>", result.getSignature());
		Assert.assertTrue(result.isIterable());
		Assert.assertEquals("java.lang.Iterable", result.getIterableType());
		Assert.assertEquals("java.lang.Object", result.getIterableOf());
		Assert.assertEquals(JavaTypeKind.Interface, result.getJavaTypeKind());

		params = new QuteResolvedJavaTypeParams("Iterable<String>", "qute-quickstart");
		result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertEquals("java.lang.Iterable<java.lang.String>", result.getSignature());
		Assert.assertTrue(result.isIterable());
		Assert.assertEquals("java.lang.Iterable", result.getIterableType());
		Assert.assertEquals("java.lang.String", result.getIterableOf());

		params = new QuteResolvedJavaTypeParams("Iterable<java.lang.String>", "qute-quickstart");
		result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertEquals("java.lang.Iterable<java.lang.String>", result.getSignature());
		Assert.assertTrue(result.isIterable());
		Assert.assertEquals("java.lang.Iterable", result.getIterableType());
		Assert.assertEquals("java.lang.String", result.getIterableOf());

		params = new QuteResolvedJavaTypeParams("java.lang.Iterable<java.lang.String>",
				"qute-quickstart");
		result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertEquals("java.lang.Iterable<java.lang.String>", result.getSignature());
		Assert.assertTrue(result.isIterable());
		Assert.assertEquals("java.lang.Iterable", result.getIterableType());
		Assert.assertEquals("java.lang.String", result.getIterableOf());
	}

	@Test
	public void testlist() throws Exception {

		QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("java.util.List",
				QuteMavenProjectName.qute_quickstart);
		ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertEquals("java.util.List<E>", result.getSignature());
		Assert.assertTrue(result.isIterable());
		Assert.assertEquals("java.util.List", result.getIterableType());
		Assert.assertEquals("java.lang.Object", result.getIterableOf());
		Assert.assertEquals(JavaTypeKind.Interface, result.getJavaTypeKind());

		// Invalid method void clear();
		JavaMethodInfo clearMethod = findMethod(result, "clear");
		Assert.assertNull(clearMethod);
		InvalidMethodReason reason = result.getInvalidMethodReason("clear");
		Assert.assertEquals(InvalidMethodReason.VoidReturn, reason);

		params = new QuteResolvedJavaTypeParams("List", QuteMavenProjectName.qute_quickstart);
		result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertNull(result);

		params = new QuteResolvedJavaTypeParams("List<String>", QuteMavenProjectName.qute_quickstart);
		result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertNull(result);

		params = new QuteResolvedJavaTypeParams("List<java.lang.String>", QuteMavenProjectName.qute_quickstart);
		result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertNull(result);

		params = new QuteResolvedJavaTypeParams("java.util.List<java.lang.String>",
				QuteMavenProjectName.qute_quickstart);
		result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertEquals("java.util.List<java.lang.String>", result.getSignature());
		Assert.assertTrue(result.isIterable());
		Assert.assertEquals("java.util.List", result.getIterableType());
		Assert.assertEquals("java.lang.String", result.getIterableOf());
	}

	@Test
	public void testsomeInterface() throws Exception {

		QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("org.acme.qute.SomeInterface",
				QuteMavenProjectName.qute_quickstart);
		ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertEquals("org.acme.qute.SomeInterface", result.getSignature());
		Assert.assertFalse(result.isIterable());

		Assert.assertNotNull(result.getMethods());
		Assert.assertEquals(1, result.getMethods().size());
		Assert.assertEquals("getName() : java.lang.String", result.getMethods().get(0).getSignature());
		Assert.assertEquals(JavaTypeKind.Interface, result.getJavaTypeKind());
	}

	@Test
	public void testitem() throws Exception {

		QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("org.acme.qute.Item",
				QuteMavenProjectName.qute_quickstart);
		ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertNotNull(result);
		Assert.assertEquals("org.acme.qute.Item", result.getSignature());
		Assert.assertFalse(result.isIterable());
		Assert.assertEquals(JavaTypeKind.Class, result.getJavaTypeKind());

		// Fields
		Assert.assertNotNull(result.getFields());
		Assert.assertEquals(2, result.getFields().size());
		Assert.assertEquals("name", result.getFields().get(0).getName());
		Assert.assertEquals("java.lang.String", result.getFields().get(0).getType());
		Assert.assertEquals("price", result.getFields().get(1).getName());
		Assert.assertEquals("java.math.BigDecimal", result.getFields().get(1).getType());

		// Methods
		Assert.assertNotNull(result.getMethods());
		Assert.assertEquals(2, result.getMethods().size());
		Assert.assertEquals("getDerivedItems() : org.acme.qute.Item[]", result.getMethods().get(0).getSignature());
		Assert.assertEquals("varArgsMethod(index : int, elements : java.lang.String...) : java.lang.String",
				result.getMethods().get(1).getSignature());

		// Invalid methods(static method)
		JavaMethodInfo discountedPriceMethod = findMethod(result, "staticMethod");
		Assert.assertNull(discountedPriceMethod);
		InvalidMethodReason reason = result.getInvalidMethodReason("staticMethod");
		Assert.assertEquals(InvalidMethodReason.Static, reason);

	}

	@Test
	public void testStatusesEnum() throws Exception {

		QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("org.acme.qute.StatusesEnum",
				QuteMavenProjectName.qute_quickstart);
		ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertNotNull(result);
		Assert.assertEquals("org.acme.qute.StatusesEnum", result.getSignature());
		Assert.assertFalse(result.isIterable());
		Assert.assertEquals(JavaTypeKind.Enum, result.getJavaTypeKind());

		// Enum
		Assert.assertNotNull(result.getFields());
		Assert.assertEquals(2, result.getFields().size());
		Assert.assertEquals("ON", result.getFields().get(0).getName());
		Assert.assertEquals("org.acme.qute.StatusesEnum", result.getFields().get(0).getType());
		Assert.assertEquals("OFF", result.getFields().get(1).getName());
		Assert.assertEquals("org.acme.qute.StatusesEnum", result.getFields().get(1).getType());

	}


	@Test
	public void testitemArray() throws Exception {

		QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("org.acme.qute.Item[]",
				QuteMavenProjectName.qute_quickstart);
		ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertNotNull(result);
		Assert.assertEquals("org.acme.qute.Item[]", result.getSignature());
		Assert.assertTrue(result.isIterable());
		Assert.assertNull(result.getIterableType());
		Assert.assertEquals("org.acme.qute.Item", result.getIterableOf());

	}

	@Test
	public void stringArray() throws Exception {

		QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("String[]",
				QuteMavenProjectName.qute_quickstart);
		ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertNotNull(result);
		Assert.assertEquals("java.lang.String[]", result.getSignature());
		Assert.assertTrue(result.isIterable());
		Assert.assertNull(result.getIterableType());
		Assert.assertEquals("java.lang.String", result.getIterableOf());
	}

	@Test
	public void testrecord() throws Exception {

		if (Integer.parseInt(System.getProperty("java.specification.version")) >= 17) {
			createMavenModule(new File("projects/qute/projects/maven/" + QuteMavenProjectName.qute_java17));

			QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("org.acme.qute.RecordItem",
					QuteMavenProjectName.qute_java17);
			ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
					new EmptyProgressIndicator());
			Assert.assertEquals("org.acme.qute.RecordItem", result.getSignature());
			Assert.assertFalse(result.isIterable());
			Assert.assertEquals(JavaTypeKind.Unknown, result.getJavaTypeKind());

			Assert.assertNotNull(result.getFields());
			Assert.assertEquals(2, result.getFields().size());
			Assert.assertEquals("name", result.getFields().get(0).getName());
			Assert.assertEquals("java.lang.String", result.getFields().get(0).getType());
			Assert.assertEquals("price", result.getFields().get(1).getName());
			Assert.assertEquals("java.math.BigDecimal", result.getFields().get(1).getType());
		}
	}

	@Test
	public void testobject() throws Exception {

		QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("java.lang.Object",
				QuteMavenProjectName.qute_quickstart);
		ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertNotNull(result);
		Assert.assertEquals("java.lang.Object", result.getSignature());
		Assert.assertFalse(result.isIterable());
		Assert.assertEquals(JavaTypeKind.Class, result.getJavaTypeKind());

		// None valid methods
		Assert.assertNotNull(result.getMethods());
		Assert.assertTrue(result.getMethods().isEmpty());

		// Invalid method codePointAt(int index)
		JavaMethodInfo waitMethod = findMethod(result, "wait");
		Assert.assertNull(waitMethod);
		InvalidMethodReason reason = result.getInvalidMethodReason("wait");
		Assert.assertEquals(InvalidMethodReason.FromObject, reason);
	}

	@Test
	public void teststring() throws Exception {

		QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("java.lang.String",
				QuteMavenProjectName.qute_quickstart);
		ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertNotNull(result);
		Assert.assertEquals("java.lang.String", result.getSignature());
		Assert.assertFalse(result.isIterable());
		Assert.assertEquals(JavaTypeKind.Class, result.getJavaTypeKind());
		Assert.assertNotNull(result.getMethods());

		// Valid method isEmpty()
		JavaMethodInfo isEmptyMethod = findMethod(result, "isEmpty");
		Assert.assertNotNull(isEmptyMethod);

		// Invalid method void getChars(int srcBegin, int srcEnd, char dst[], int
		// dstBegin) {
		JavaMethodInfo getCharsMethod = findMethod(result, "getChars");
		Assert.assertNull(getCharsMethod);
		InvalidMethodReason reason = result.getInvalidMethodReason("getChars");
		Assert.assertEquals(InvalidMethodReason.VoidReturn, reason);

		// Extended types
		// public final class String implements java.io.Serializable,
		// Comparable<String>, CharSequence {
		List<String> extendedTypes = result.getExtendedTypes();
		Assert.assertNotNull(extendedTypes);
		assertExtendedTypes("java.lang.String", "java.io.Serializable", extendedTypes);
		assertExtendedTypes("java.lang.String", "java.lang.CharSequence", extendedTypes);
	}

	@Test
	public void testtemplateData() throws Exception {

		QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("org.acme.qute.ItemWithTemplateData",
				QuteMavenProjectName.qute_quickstart);
		ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertNotNull(result);
		Assert.assertEquals("org.acme.qute.ItemWithTemplateData", result.getSignature());
		Assert.assertFalse(result.isIterable());
		Assert.assertEquals(JavaTypeKind.Class, result.getJavaTypeKind());

		// @TemplateData

		// @TemplateData(target = BigDecimal.class)
		// @TemplateData(ignoreSuperclasses = true)
		// public class ItemWithTemplateData {
		Assert.assertNotNull(result.getTemplateDataAnnotations());
		Assert.assertEquals(2, result.getTemplateDataAnnotations().size());
		// @TemplateData(target = BigDecimal.class)
		Assert.assertFalse(result.getTemplateDataAnnotations().get(0).isIgnoreSuperclasses());
		// @TemplateData(ignoreSuperclasses = true)
		Assert.assertTrue(result.getTemplateDataAnnotations().get(1).isIgnoreSuperclasses());

		// Fields
		Assert.assertNotNull(result.getFields());
		Assert.assertEquals(3, result.getFields().size());
		Assert.assertEquals("name", result.getFields().get(0).getName());
		Assert.assertEquals("java.lang.String", result.getFields().get(0).getType());
		Assert.assertEquals("price", result.getFields().get(1).getName());
		Assert.assertEquals("java.math.BigDecimal", result.getFields().get(1).getType());
		Assert.assertEquals("count", result.getFields().get(2).getName());
		Assert.assertEquals("java.lang.String", result.getFields().get(2).getType());

		// Methods
		Assert.assertNotNull(result.getMethods());
		Assert.assertEquals(1, result.getMethods().size());
		Assert.assertEquals("getDerivedItems() : org.acme.qute.Item[]", result.getMethods().get(0).getSignature());

		// Invalid methods(static method)
		JavaMethodInfo discountedPriceMethod = findMethod(result, "staticMethod");
		Assert.assertNull(discountedPriceMethod);
		InvalidMethodReason reason = result.getInvalidMethodReason("staticMethod");
		Assert.assertEquals(InvalidMethodReason.Static, reason);
	}

	@Test
	public void testtemplateDataStatic() throws Exception {

		QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("org.acme.qute.Statuses",
				QuteMavenProjectName.qute_quickstart);
		ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertNotNull(result);
		Assert.assertEquals("org.acme.qute.Statuses", result.getSignature());
		Assert.assertFalse(result.isIterable());
		Assert.assertEquals(JavaTypeKind.Class, result.getJavaTypeKind());

		// @TemplateData
		// @TemplateData(namespace = "FOO")
		// @TemplateData(namespace = "BAR")
		// public class Statuses {
		Assert.assertNotNull(result.getTemplateDataAnnotations());
		Assert.assertEquals(3, result.getTemplateDataAnnotations().size());
		// @TemplateData
		Assert.assertFalse(result.getTemplateDataAnnotations().get(0).isIgnoreSuperclasses());
		// @TemplateData(namespace = "FOO")
		Assert.assertFalse(result.getTemplateDataAnnotations().get(1).isIgnoreSuperclasses());
		// @TemplateData(namespace = "BAR")
		Assert.assertFalse(result.getTemplateDataAnnotations().get(2).isIgnoreSuperclasses());

		// Fields
		Assert.assertNotNull(result.getFields());
		Assert.assertEquals(2, result.getFields().size());
		Assert.assertEquals("ON", result.getFields().get(0).getName());
		Assert.assertEquals("java.lang.String", result.getFields().get(0).getType());
		Assert.assertEquals("OFF", result.getFields().get(1).getName());
		Assert.assertEquals("java.lang.String", result.getFields().get(1).getType());

		// Invalid methods(static method)
		Assert.assertNull(findMethod(result, "staticMethod"));
		InvalidMethodReason reason = result.getInvalidMethodReason("staticMethod");
		Assert.assertEquals(InvalidMethodReason.Static, reason);
	}

	@Test
	public void testregisterForReflection() throws Exception {

		QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("org.acme.qute.ItemWithRegisterForReflection",
				QuteMavenProjectName.qute_quickstart);
		ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertNotNull(result);
		Assert.assertEquals("org.acme.qute.ItemWithRegisterForReflection", result.getSignature());
		Assert.assertFalse(result.isIterable());
		Assert.assertEquals(JavaTypeKind.Class, result.getJavaTypeKind());

		// @RegisterForReflection

		// @RegisterForReflection(fields = false)
		// public class ItemWithRegisterForReflection {
		Assert.assertNotNull(result.getRegisterForReflectionAnnotation());
		Assert.assertFalse(result.getRegisterForReflectionAnnotation().isFields());
		Assert.assertTrue(result.getRegisterForReflectionAnnotation().isMethods());

		// Fields
		Assert.assertNotNull(result.getFields());
		Assert.assertEquals(2, result.getFields().size());
		Assert.assertEquals("name", result.getFields().get(0).getName());
		Assert.assertEquals("java.lang.String", result.getFields().get(0).getType());
		Assert.assertEquals("price", result.getFields().get(1).getName());
		Assert.assertEquals("java.math.BigDecimal", result.getFields().get(1).getType());

		// Methods
		Assert.assertNotNull(result.getMethods());
		Assert.assertEquals(1, result.getMethods().size());
		Assert.assertEquals("getDerivedItems() : org.acme.qute.Item[]", result.getMethods().get(0).getSignature());

		// Invalid methods(static method)
		JavaMethodInfo discountedPriceMethod = findMethod(result, "staticMethod");
		Assert.assertNull(discountedPriceMethod);
		InvalidMethodReason reason = result.getInvalidMethodReason("staticMethod");
		Assert.assertEquals(InvalidMethodReason.Static, reason);
	}

	@Test
	public void testIgnoreSyntheticMethod() throws Exception {

		QuteResolvedJavaTypeParams params = new QuteResolvedJavaTypeParams("java.lang.CharSequence",
				QuteMavenProjectName.qute_quickstart);
		ResolvedJavaTypeInfo result = QuteSupportForTemplate.getInstance().getResolvedJavaType(params, getJDTUtils(),
				new EmptyProgressIndicator());
		Assert.assertNotNull(result);

		// lambda$chars$0 should be ignored
		Assert.assertEquals("java.lang.CharSequence", result.getSignature());
		JavaMethodInfo syntheticMethod = findMethod(result, "lambda$chars$0");
		Assert.assertNull(syntheticMethod);
	}

	private static void assertExtendedTypes(String type, String extendedType, List<String> extendedTypes) {
		Assert.assertTrue("The Java type '" + type + "' should extends '" + extendedType + "'.",
				extendedTypes.contains(extendedType));
	}

	private static JavaMethodInfo findMethod(ResolvedJavaTypeInfo javaType, String methodName) {
		Optional<JavaMethodInfo> result = javaType.getMethods().stream() //
				.filter(m -> methodName.equals(m.getName())) //
				.findFirst();
		return result.isPresent() ? result.get() : null;
	}

}
