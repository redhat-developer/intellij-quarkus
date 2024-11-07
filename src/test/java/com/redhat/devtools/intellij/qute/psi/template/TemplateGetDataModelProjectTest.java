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

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.qute.psi.QuteMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.qute.psi.QuteMavenProjectName;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelProject;
import com.redhat.qute.commons.datamodel.DataModelTemplate;
import com.redhat.qute.commons.datamodel.QuteDataModelProjectParams;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.redhat.devtools.intellij.qute.psi.QuteAssert.*;

/**
 * Tests for
 * {@link QuteSupportForTemplate#getDataModelProject(QuteDataModelProjectParams, com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils, com.intellij.openapi.progress.ProgressIndicator)}
 *
 * @author Angelo ZERR
 */
public class TemplateGetDataModelProjectTest extends QuteMavenModuleImportingTestCase {

    @Test
    public void testQuteQuickStart() throws Exception {

        loadMavenProject(QuteMavenProjectName.qute_quickstart);

        QuteDataModelProjectParams params = new QuteDataModelProjectParams(QuteMavenProjectName.qute_quickstart);
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

        // hello.data("height", 1.50, "weight", 50L)
        // .data("age", 12)
        // .data("name", name);

        Assert.assertEquals(4, parameters.size());
        assertParameter("height", "double", true, parameters, 0);
        assertParameter("weight", "long", true, parameters, 1);
        assertParameter("age", "int", true, parameters, 2);
        assertParameter("name", "java.lang.String", true, parameters, 3);

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
        assertParameter("age2", "int", true, goodbyeTemplate);
        assertParameter("name2", "java.lang.String", true, goodbyeTemplate);

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
        assertParameter("age3", "int", true, halloTemplate);
        assertParameter("name3", "java.lang.String", true, halloTemplate);

    }

    private static void checkedTemplateInnerClass(DataModelProject<DataModelTemplate<DataModelParameter>> project) {
        // static native TemplateInstance items(List<Item> items);
        DataModelTemplate<DataModelParameter> items = project
                .findDataModelTemplate("src/main/resources/templates/ItemResource/items");
        Assert.assertNotNull(items);
        Assert.assertEquals("src/main/resources/templates/ItemResource/items", items.getTemplateUri());
        Assert.assertEquals("org.acme.qute.ItemResource$Templates", items.getSourceType());
        Assert.assertEquals("items", items.getSourceMethod());

        List<DataModelParameter> parameters = items.getParameters();
        Assert.assertNotNull(parameters);

        Assert.assertEquals(1, parameters.size());
        assertParameter("items", "java.util.List<org.acme.qute.Item>", false, parameters, 0);

        // static native TemplateInstance map(Map<String, List<Item>> items,
        // Map.Entry<String, Integer> entry);

        DataModelTemplate<DataModelParameter> map = project
                .findDataModelTemplate("src/main/resources/templates/ItemResource/map");
        Assert.assertNotNull(map);
        Assert.assertEquals("src/main/resources/templates/ItemResource/map", map.getTemplateUri());
        Assert.assertEquals("org.acme.qute.ItemResource$Templates", map.getSourceType());
        Assert.assertEquals("map", map.getSourceMethod());

        parameters = map.getParameters();
        Assert.assertNotNull(parameters);

        Assert.assertEquals(2, parameters.size());
        assertParameter("items", "java.util.Map<java.lang.String,java.util.List<org.acme.qute.Item>>", false,
                parameters, 0);
        assertParameter("entry", "java.util.Map$Entry<java.lang.String,java.lang.Integer>", false, parameters, 1);
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
        assertParameter("name", "java.lang.String", false, hello2Parameters, 0);

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
        assertParameter("name", "java.lang.String", false, hello3Parameters, 0);
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
                "io.quarkus.qute.runtime.extensions.ConfigTemplateExtensions",
                Arrays.asList("*"), resolvers);
        assertValueResolver("config", "booleanProperty(propertyName : java.lang.String) : java.lang.Object",
                "io.quarkus.qute.runtime.extensions.ConfigTemplateExtensions",
                Arrays.asList("boolean"), resolvers);

        // from io.quarkus.qute.runtime.extensions.MapTemplateExtensions
        assertValueResolver(null, "map(map : java.util.Map, name : java.lang.String) : java.lang.Object",
                "io.quarkus.qute.runtime.extensions.MapTemplateExtensions",
                Arrays.asList("*"), resolvers);
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

        // @Named
        // public @interface IgnoreInjectAnnotation
        assertNotValueResolver("inject", "org.acme.qute.IgnoreInjectAnnotation", "org.acme.qute.IgnoreInjectAnnotation", //
                "ignoreInjectAnnotation", resolvers);

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
        assertValueResolver("global", "age : int", "org.acme.qute.Globals", null, true, resolvers);

        // static String name;
        assertValueResolver("global", "name : java.lang.String", "org.acme.qute.Globals", null, true, resolvers);

        // static Color[] myColors() {
        // return new Color[] { Color.RED, Color.BLUE };
        // }
        assertValueResolver("global", "myColors() : org.acme.qute.Color[]", "org.acme.qute.Globals", null, true, resolvers);

        // @TemplateGlobal(name = "currentUser")
        // static String user() {
        // return "Mia";
        // }
        assertValueResolver("global", "user() : java.lang.String", "org.acme.qute.Globals", "currentUser", true, resolvers);
        // }
    }

    @Test
    public void testQuarkus3() throws Exception {
        loadMavenProject(QuteMavenProjectName.quarkus3);

        QuteDataModelProjectParams params = new QuteDataModelProjectParams(QuteMavenProjectName.quarkus3);
        DataModelProject<DataModelTemplate<DataModelParameter>> project = QuteSupportForTemplate.getInstance()
                .getDataModelProject(params, getJDTUtils(), new EmptyProgressIndicator());
        Assert.assertNotNull(project);

        // Test value resolvers
        List<ValueResolverInfo> resolvers = project.getValueResolvers();

        // should pick up the named bean
        assertValueResolver("inject", "org.acme.Bean2", "org.acme.Bean2", resolvers);
    }

    @Test
    public void testQuteRecord() throws Exception {
        loadMavenProject(QuteMavenProjectName.qute_record);

        QuteDataModelProjectParams params = new QuteDataModelProjectParams(QuteMavenProjectName.qute_record);
        DataModelProject<DataModelTemplate<DataModelParameter>> project = QuteSupportForTemplate.getInstance()
                .getDataModelProject(params, getJDTUtils(), new EmptyProgressIndicator());
        Assert.assertNotNull(project);

        // public class HelloResource {

        // record Hello(String name) implements TemplateInstance {}

        // record Bonjour(String name) implements TemplateInstance {}

        // record Status() {}

        // @CheckedTemplate(basePath="Foo", defaultName=CheckedTemplate.HYPHENATED_ELEMENT_NAME)
        // record HelloWorld(String name) implements TemplateInstance {}

        // Hello ->
        DataModelTemplate<DataModelParameter> helloTemplate = project
                .findDataModelTemplate("src/main/resources/templates/HelloResource/Hello");
        Assert.assertNotNull(helloTemplate);
        Assert.assertEquals("src/main/resources/templates/HelloResource/Hello", helloTemplate.getTemplateUri());
        Assert.assertEquals("org.acme.sample.HelloResource.Hello", helloTemplate.getSourceType());
        Assert.assertNull(helloTemplate.getSourceField());
        Assert.assertNull(helloTemplate.getSourceMethod());

        List<DataModelParameter> helloParameters = helloTemplate.getParameters();
        Assert.assertNotNull(helloParameters);

        Assert.assertEquals(2, helloParameters.size());

        // record Hello(String name) implements TemplateInstance {}
        assertParameter("name", "java.lang.String", false, helloParameters, 0);

        //  public TemplateInstance get(@QueryParam("name") String name) {
        // return new Hello(name).data("foo", 100);
        assertParameter("foo", "int", true, helloParameters, 1);

        // HelloWorld ->
        // @CheckedTemplate(basePath="Foo",
        // defaultName=CheckedTemplate.HYPHENATED_ELEMENT_NAME)
        // record HelloWorld(String name) implements TemplateInstance {}
        DataModelTemplate<DataModelParameter> helloWorldTemplate = project
                .findDataModelTemplate("src/main/resources/templates/Foo/hello-world");
        Assert.assertNotNull(helloWorldTemplate);
        Assert.assertEquals("src/main/resources/templates/Foo/hello-world", helloWorldTemplate.getTemplateUri());
        Assert.assertEquals("org.acme.sample.HelloResource.HelloWorld", helloWorldTemplate.getSourceType());
        Assert.assertNull(helloWorldTemplate.getSourceField());
        Assert.assertNull(helloWorldTemplate.getSourceMethod());

        List<DataModelParameter> helloWorldParameters = helloWorldTemplate.getParameters();
        Assert.assertNotNull(helloWorldParameters);

        Assert.assertEquals(1, helloWorldParameters.size());

        // record HelloWorld(String name) implements TemplateInstance {}
        assertParameter("name", "java.lang.String", false, helloWorldParameters, 0);
    }

    @Test
    public void testCheckedTemplateWithDefaultName() throws Exception {
        loadMavenProject(QuteMavenProjectName.qute_record);

        QuteDataModelProjectParams params = new QuteDataModelProjectParams(QuteMavenProjectName.qute_record);
        DataModelProject<DataModelTemplate<DataModelParameter>> project = QuteSupportForTemplate.getInstance()
                .getDataModelProject(params, getJDTUtils(), new EmptyProgressIndicator());
        Assert.assertNotNull(project);

        // public class HelloResource {
        // record Hello(String name) implements TemplateInstance {}

        // Hello
        DataModelTemplate<DataModelParameter> helloTemplate = project
                .findDataModelTemplate("src/main/resources/templates/ItemResource/hello-world.html");
        Assert.assertNotNull(helloTemplate);
        Assert.assertEquals("src/main/resources/templates/ItemResource/hello-world", helloTemplate.getTemplateUri());
        Assert.assertEquals("org.acme.sample.ItemResource$Templates2", helloTemplate.getSourceType());
        Assert.assertEquals("HelloWorld", helloTemplate.getSourceMethod());
        Assert.assertNull(helloTemplate.getSourceField());

        List<DataModelParameter> helloParameters = helloTemplate.getParameters();
        Assert.assertNotNull(helloParameters);

        Assert.assertEquals(1, helloParameters.size());

        // static class Templates2 {
        // static native TemplateInstance HelloWorld(String name);
        assertParameter("name", "java.lang.String", false, helloParameters, 0);

    }

}
