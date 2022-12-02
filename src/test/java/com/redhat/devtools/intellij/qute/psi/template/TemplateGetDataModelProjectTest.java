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
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import org.junit.Assert;
import org.junit.Test;

import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelProject;
import com.redhat.qute.commons.datamodel.DataModelTemplate;
import com.redhat.qute.commons.datamodel.QuteDataModelProjectParams;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;

/**
 * Tests for
 * {@link QuteSupportForTemplate#getDataModelProject(QuteDataModelProjectParams, com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils, com.intellij.openapi.progress.ProgressIndicator)}
 *
 * @author Angelo ZERR
 *
 */
public class TemplateGetDataModelProjectTest extends MavenModuleImportingTestCase {

	@Test
	public void testquteQuickStart() throws Exception {

		Module module = createMavenModule(new File("projects/qute/projects/maven/qute-quickstart"));

		QuteDataModelProjectParams params = new QuteDataModelProjectParams("qute-quickstart");
		DataModelProject<DataModelTemplate<DataModelParameter>> project = QuteSupportForTemplate.getInstance()
				.getDataModelProject(params, PsiUtilsLSImpl.getInstance(myProject), new EmptyProgressIndicator());
		Assert.assertNotNull(project);

		// Test templates
		testTemplates(project);
		// Test value resolvers

		List<ValueResolverInfo> resolvers = project.getValueResolvers();
		Assert.assertNotNull(resolvers);
		Assert.assertFalse(resolvers.isEmpty());

		testValueResolversFromTemplateExtension(resolvers);
		testValueResolversFromInject(resolvers);
		testValueResolversFromTemplateData(resolvers);
		testValueResolversFromTemplateEnum(resolvers);
		testValueResolversFromTemplateGlobal(resolvers);
	}

	private static void testTemplates(DataModelProject<DataModelTemplate<DataModelParameter>> project) {
		List<DataModelTemplate<DataModelParameter>> templates = project.getTemplates();
		Assert.assertNotNull(templates);
		Assert.assertFalse(templates.isEmpty());

		templateField(project);
		checkedTemplateInnerClass(project);
		checkedTemplate(project);
	}

	private static void templateField(DataModelProject<DataModelTemplate<DataModelParameter>> project) {
		// Template hello;

		DataModelTemplate<DataModelParameter> helloTemplate = project
				.findDataModelTemplate("src/main/resources/templates/hello");
		Assert.assertNotNull(helloTemplate);
		Assert.assertEquals("src/main/resources/templates/hello", helloTemplate.getTemplateUri());
		Assert.assertEquals("org.acme.qute.HelloResource", helloTemplate.getSourceType());
		Assert.assertEquals("hello", helloTemplate.getSourceField());

		List<DataModelParameter> parameters = helloTemplate.getParameters();
		Assert.assertNotNull(parameters);

		// hello.data("age", 12);
		// hello.data("height", 1.50, "weight", 50.5);
		// return hello.data("name", name);

		Assert.assertEquals(4, parameters.size());
		assertParameter("name", "java.lang.String", true, parameters);
		assertParameter("height", "double", true, parameters);
		assertParameter("weight", "long", true, parameters);
		assertParameter("age", "int", true, parameters);

		// Template goodbye;

		DataModelTemplate<DataModelParameter> goodbyeTemplate = project
				.findDataModelTemplate("src/main/resources/templates/goodbye");
		Assert.assertNotNull(goodbyeTemplate);
		Assert.assertEquals("src/main/resources/templates/goodbye", goodbyeTemplate.getTemplateUri());
		Assert.assertEquals("org.acme.qute.HelloResource", goodbyeTemplate.getSourceType());
		Assert.assertEquals("goodbye", goodbyeTemplate.getSourceField());

		List<DataModelParameter> parameters2 = goodbyeTemplate.getParameters();
		Assert.assertNotNull(parameters2);

		// goodbye.data("age2", 12);
		// return goodbye.data("name2", name);

		Assert.assertEquals(2, parameters2.size());
		assertParameter("age2", "int", true, parameters2);
		assertParameter("name2", "java.lang.String", true, parameters2);

		// Template hallo;

		DataModelTemplate<DataModelParameter> halloTemplate = project
				.findDataModelTemplate("src/main/resources/templates/detail/items2_v1.html");
		Assert.assertNotNull(halloTemplate);
		Assert.assertEquals("src/main/resources/templates/detail/items2_v1.html", halloTemplate.getTemplateUri());
		Assert.assertEquals("org.acme.qute.HelloResource", halloTemplate.getSourceType());
		Assert.assertEquals("hallo", halloTemplate.getSourceField());

		List<DataModelParameter> parameters3 = halloTemplate.getParameters();
		Assert.assertNotNull(parameters3);

		// hallo.data("age3", 12);
		// return hallo.data("name3", name);

		Assert.assertEquals(2, parameters3.size());
		assertParameter("age3", "int", true, parameters3);
		assertParameter("name3", "java.lang.String", true, parameters3);

	}

	private static void checkedTemplateInnerClass(DataModelProject<DataModelTemplate<DataModelParameter>> project) {
		DataModelTemplate<DataModelParameter> itemResourceTemplate = project
				.findDataModelTemplate("src/main/resources/templates/ItemResource/items");
		Assert.assertNotNull(itemResourceTemplate);
		Assert.assertEquals("src/main/resources/templates/ItemResource/items", itemResourceTemplate.getTemplateUri());
		Assert.assertEquals("org.acme.qute.ItemResource$Templates", itemResourceTemplate.getSourceType());
		Assert.assertEquals("items", itemResourceTemplate.getSourceMethod());

		List<DataModelParameter> parameters = itemResourceTemplate.getParameters();
		Assert.assertNotNull(parameters);

		// static native TemplateInstance items(List<Item> items);

		Assert.assertEquals(1, parameters.size());
		assertParameter("items", "java.util.List<org.acme.qute.Item>", false, parameters);
	}

	private static void checkedTemplate(DataModelProject<DataModelTemplate<DataModelParameter>> project) {
		// hello2
		DataModelTemplate<DataModelParameter> hello2Template = project
				.findDataModelTemplate("src/main/resources/templates/hello2");
		Assert.assertNotNull(hello2Template);
		Assert.assertEquals("src/main/resources/templates/hello2", hello2Template.getTemplateUri());
		Assert.assertEquals("org.acme.qute.Templates", hello2Template.getSourceType());
		Assert.assertEquals("hello2", hello2Template.getSourceMethod());

		List<DataModelParameter> hello2Parameters = hello2Template.getParameters();
		Assert.assertNotNull(hello2Parameters);

		// public static native TemplateInstance hello2(String name);

		Assert.assertEquals(1, hello2Parameters.size());
		assertParameter("name", "java.lang.String", false, hello2Parameters);

		// hello3
		DataModelTemplate<DataModelParameter> hello3Template = project
				.findDataModelTemplate("src/main/resources/templates/hello3");
		Assert.assertNotNull(hello3Template);
		Assert.assertEquals("src/main/resources/templates/hello3", hello3Template.getTemplateUri());
		Assert.assertEquals("org.acme.qute.Templates", hello3Template.getSourceType());
		Assert.assertEquals("hello3", hello3Template.getSourceMethod());

		List<DataModelParameter> hello3Parameters = hello3Template.getParameters();
		Assert.assertNotNull(hello3Parameters);

		// public static native TemplateInstance hello3(String name);

		Assert.assertEquals(1, hello3Parameters.size());
		assertParameter("name", "java.lang.String", false, hello3Parameters);
	}

	private static void testValueResolversFromTemplateExtension(List<ValueResolverInfo> resolvers) {

		// Resolver from Java sources
		assertValueResolver(null, "discountedPrice(item : org.acme.qute.Item) : java.math.BigDecimal",
				"org.acme.qute.ItemResource", resolvers);
		// Resolver from Java binaries
		// from io.quarkus.qute.runtime.extensions.CollectionTemplateExtensions
		assertValueResolver(null, "get(list : java.util.List<T>, index : int) : T",
				"io.quarkus.qute.runtime.extensions.CollectionTemplateExtensions", resolvers);
		assertValueResolver(null, "getByIndex(list : java.util.List<T>, index : java.lang.String) : T",
				"io.quarkus.qute.runtime.extensions.CollectionTemplateExtensions", resolvers);

		// from io.quarkus.qute.runtime.extensions.ConfigTemplateExtensions
		assertValueResolver("config", "getConfigProperty(propertyName : java.lang.String) : java.lang.Object",
				"io.quarkus.qute.runtime.extensions.ConfigTemplateExtensions", resolvers);

		// from io.quarkus.qute.runtime.extensions.MapTemplateExtensions
		assertValueResolver(null, "map(map : java.util.Map, name : java.lang.String) : java.lang.Object",
				"io.quarkus.qute.runtime.extensions.MapTemplateExtensions", resolvers);
		assertValueResolver(null, "get(map : java.util.Map<?,V>, key : java.lang.Object) : V",
				"io.quarkus.qute.runtime.extensions.MapTemplateExtensions", resolvers);

		// from io.quarkus.qute.runtime.extensions.StringTemplateExtensions
		assertValueResolver(null, "mod(number : java.lang.Integer, mod : java.lang.Integer) : java.lang.Integer",
				"io.quarkus.qute.runtime.extensions.NumberTemplateExtensions", resolvers);

		// from io.quarkus.qute.runtime.extensions.StringTemplateExtensions
		assertValueResolver(null,
				"fmtInstance(format : java.lang.String, ignoredPropertyName : java.lang.String, args : java.lang.Object...) : java.lang.String",
				"io.quarkus.qute.runtime.extensions.StringTemplateExtensions", resolvers);
		assertValueResolver("str",
				"fmt(ignoredPropertyName : java.lang.String, format : java.lang.String, args : java.lang.Object...) : java.lang.String",
				"io.quarkus.qute.runtime.extensions.StringTemplateExtensions", resolvers);

		// from io.quarkus.qute.runtime.extensions.TimeTemplateExtensions
		assertValueResolver(null,
				"format(temporal : java.time.temporal.TemporalAccessor, pattern : java.lang.String) : java.lang.String",
				"io.quarkus.qute.runtime.extensions.TimeTemplateExtensions", resolvers);
		assertValueResolver("time",
				"format(dateTimeObject : java.lang.Object, pattern : java.lang.String) : java.lang.String",
				"io.quarkus.qute.runtime.extensions.TimeTemplateExtensions", resolvers);

	}

	private void testValueResolversFromInject(List<ValueResolverInfo> resolvers) {
		Assert.assertNotNull(resolvers);
		Assert.assertFalse(resolvers.isEmpty());

		// from org.acme.qute.InjectedData source

		// @Named
		// public class InjectedData;
		assertValueResolver("inject", "org.acme.qute.InjectedData", "org.acme.qute.InjectedData", //
				"injectedData", resolvers);

		// @Named
		// private String foo;
		assertValueResolver("inject", "foo : java.lang.String", "org.acme.qute.InjectedData", //
				"foo", resolvers);

		// @Named("bar")
		// private String aBar;
		assertValueResolver("inject", "aBar : java.lang.String", "org.acme.qute.InjectedData", //
				"bar", resolvers);

		// @Named("user")
		// private String getUser() {...
		assertValueResolver("inject", "getUser() : java.lang.String", "org.acme.qute.InjectedData", //
				"user", resolvers);

		// @Named
		// private String getSystemUser() {...
		assertValueResolver("inject", "getSystemUser() : java.lang.String", "org.acme.qute.InjectedData", //
				"systemUser", resolvers);

		// from io.quarkus.vertx.http.runtime.CurrentRequestProducer
		assertValueResolver("inject",
				"getCurrentRequest(rc : io.vertx.ext.web.RoutingContext) : io.vertx.core.http.HttpServerRequest",
				"io.quarkus.vertx.http.runtime.CurrentRequestProducer", //
				"vertxRequest", resolvers);

	}

	private static void testValueResolversFromTemplateData(List<ValueResolverInfo> resolvers) {

		// @TemplateData(target = BigDecimal.class)
		// @TemplateData(ignoreSuperclasses = true)
		// public class ItemWithTemplateData {

		// public static BigDecimal staticMethod(Item item) {
		assertValueResolver("org_acme_qute_ItemWithTemplateData",
				"staticMethod(item : org.acme.qute.Item) : java.math.BigDecimal", "org.acme.qute.ItemWithTemplateData",
				resolvers);

		// public static String count;
		assertValueResolver("org_acme_qute_ItemWithTemplateData", "count : java.lang.String",
				"org.acme.qute.ItemWithTemplateData", resolvers);

		// @TemplateData
		// @TemplateData(namespace = "FOO")
		// @TemplateData(namespace = "BAR")
		// public class Statuses {

		// public static final String ON = "on";
		assertValueResolver("FOO", "ON : java.lang.String", "org.acme.qute.Statuses", resolvers);

		// public static final String OFF = "off";
		assertValueResolver("FOO", "OFF : java.lang.String", "org.acme.qute.Statuses", resolvers);

		// public static String staticMethod(String state) {
		assertValueResolver("FOO", "staticMethod(state : java.lang.String) : java.lang.String",
				"org.acme.qute.Statuses", resolvers);
	}

	private static void testValueResolversFromTemplateEnum(List<ValueResolverInfo> resolvers) {

		// @TemplateEnum
		// public enum StatusesEnum {

		// ON,
		assertValueResolver("StatusesEnum", "ON : org.acme.qute.StatusesEnum", "org.acme.qute.StatusesEnum", resolvers);
		// OFF
		assertValueResolver("StatusesEnum", "OFF : org.acme.qute.StatusesEnum", "org.acme.qute.StatusesEnum",
				resolvers);
	}

	private static void testValueResolversFromTemplateGlobal(List<ValueResolverInfo> resolvers) {

		// @TemplateGlobal
		// public class Globals {

		// static int age = 40;
		assertValueResolver(null, "age : int", "org.acme.qute.Globals", null, true, resolvers);

		// static String name;
		assertValueResolver(null, "name : java.lang.String", "org.acme.qute.Globals", null, true, resolvers);

		// static Color[] myColors() {
		// return new Color[] { Color.RED, Color.BLUE };
		// }
		assertValueResolver(null, "myColors() : org.acme.qute.Color[]", "org.acme.qute.Globals", null, true, resolvers);

		// @TemplateGlobal(name = "currentUser")
		// static String user() {
		// return "Mia";
		// }
		assertValueResolver(null, "user() : java.lang.String", "org.acme.qute.Globals", "currentUser", true, resolvers);
		// }
	}

	private static void assertValueResolver(String namespace, String signature, String sourceType,
			List<ValueResolverInfo> resolvers) {
		assertValueResolver(namespace, signature, sourceType, null, resolvers);
	}

	private static void assertValueResolver(String namespace, String signature, String sourceType, String named,
											List<ValueResolverInfo> resolvers) {
		assertValueResolver(namespace, signature, sourceType, named, false, resolvers);
	}

	private static void assertValueResolver(String namespace, String signature, String sourceType, String named,
			boolean globalVariable, List<ValueResolverInfo> resolvers) {
		Optional<ValueResolverInfo> result = resolvers.stream().filter(r -> signature.equals(r.getSignature()))
				.findFirst();
		Assert.assertFalse("Find '" + signature + "' value resolver.", result.isEmpty());
		ValueResolverInfo resolver = result.get();
		Assert.assertEquals(namespace, resolver.getNamespace());
		Assert.assertEquals(signature, resolver.getSignature());
		Assert.assertEquals(sourceType, resolver.getSourceType());
		Assert.assertEquals(globalVariable, resolver.isGlobalVariable());
	}

	private static void assertParameter(String key, String sourceType, boolean dataMethodInvocation,
			List<DataModelParameter> parameters) {
		DataModelParameter parameter = parameters.stream().filter(p -> key.equals(p.getKey())).findFirst().get();
		Assert.assertEquals(key, parameter.getKey());
		Assert.assertEquals(sourceType, parameter.getSourceType());
		Assert.assertEquals(dataMethodInvocation, parameter.isDataMethodInvocation());
	}

}
