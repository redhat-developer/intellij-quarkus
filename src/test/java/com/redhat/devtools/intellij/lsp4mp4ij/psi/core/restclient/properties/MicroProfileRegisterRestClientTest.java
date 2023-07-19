/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.restclient.properties;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.LSP4MPMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileMavenProjectName;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.*;

/**
 * Test collect MicroProfile properties from @RegisterRestClient
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/MicroProfileRegisterRestClientTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/MicroProfileRegisterRestClientTest.java</a>
 */
public class MicroProfileRegisterRestClientTest extends LSP4MPMavenModuleImportingTestCase {

    @Test
    public void testRestClientQuickstart() throws Exception {

        Module module = loadMavenProject(MicroProfileMavenProjectName.rest_client_quickstart);
        MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.ONLY_SOURCES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.PlainText, new EmptyProgressIndicator());

        // mp-rest Properties
        assertProperties(infoFromClasspath, 7,

                p(null, "${mp.register.rest.client.class}/mp-rest/url", "java.lang.String",
                        "The base URL to use for this service, the equivalent of the `baseUrl` method.\r\n"
                                + "This property (or */mp-rest/uri) is considered required, however implementations may have other ways to define these URLs/URIs.",
                        false, null, null, null, 0, null),

                p(null, "${mp.register.rest.client.class}/mp-rest/uri", "java.lang.String",
                        "The base URI to use for this service, the equivalent of the baseUri method.\r\n"
                                + "This property (or */mp-rest/url) is considered required, however implementations may have other ways to define these URLs/URIs."
                                + "This property will override any `baseUri` value specified in the `@RegisterRestClient` annotation.",
                        false, null, null, null, 0, null),

                p(null, "${mp.register.rest.client.class}/mp-rest/scope", "java.lang.String",
                        "The fully qualified classname to a CDI scope to use for injection, defaults to "
                                + "`javax.enterprise.context.Dependent`.",
                        false, null, null, null, 0, null),

                p(null, "${mp.register.rest.client.class}/mp-rest/providers", "java.lang.String",
                        "A comma separated list of fully-qualified provider classnames to include in the client, "
                                + "the equivalent of the `register` method or the `@RegisterProvider` annotation.",
                        false, null, null, null, 0, null),

                p(null, "${mp.register.rest.client.class}/mp-rest/connectTimeout", "long",
                        "Timeout specified in milliseconds to wait to connect to the remote endpoint.", false, null,
                        null, null, 0, null),

                p(null, "${mp.register.rest.client.class}/mp-rest/readTimeout", "long",
                        "Timeout specified in milliseconds to wait for a response from the remote endpoint.", false,
                        null, null, null, 0, null),

                p(null, "${mp.register.rest.client.class}/mp-rest/providers/{*}/priority", "int",
                        "Override the priority of the provider for the given interface.", false, null, null, null, 0,
                        null));

        assertPropertiesDuplicate(infoFromClasspath);

        // mp-rest Hints
        assertHints(infoFromClasspath, 4,

                h("${mp.register.rest.client.class}", null, false, null,
                        vh("org.acme.restclient.CountriesService", null, "org.acme.restclient.CountriesService"), //
                        vh("configKey", null, "org.acme.restclient.CountiesServiceWithConfigKey")));

        assertHintsDuplicate(infoFromClasspath);
    }
}
