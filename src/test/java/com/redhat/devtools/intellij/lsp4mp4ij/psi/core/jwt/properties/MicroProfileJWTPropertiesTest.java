/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jwt.properties;

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

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.p;

/**
 * Test the availability of the MicroProfile JWT properties
 *
 * @author Kathryn Kodama
 */
public class MicroProfileJWTPropertiesTest extends LSP4MPMavenModuleImportingTestCase {

    @Test
    public void testMicroprofileJWT() throws Exception {

        Module module = loadMavenProject(MicroProfileMavenProjectName.microprofile_jwt_quickstart);
        MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.PlainText, new EmptyProgressIndicator());

        assertProperties(infoFromClasspath,
                // confirm properties are being merged with force, should not overwrite properties coming from ConfigProperty provider

                p(null, "mp.jwt.verify.issuer", "java.lang.String", null,
                        true, "io.smallrye.jwt.config.JWTAuthContextInfoProvider", "mpJwtIssuer", null, 0, "NONE"),

                // properties coming from static JSON

                p("microprofile-jwt-api", "mp.jwt.verify.publickey.algorithm", "java.lang.String",
                        "Configuration property to specify the Public Key Signature Algorithm property. The value can be set to either `RS256` or `ES256`, `RS256` is the default value.",
                        true, null, null, null, 0, null),

                p("microprofile-jwt-api", "mp.jwt.decrypt.key.location", "java.lang.String",
                        "Configuration property to specify the relative path or full URL of the decryption key.",
                        true, null, null, null, 0, null),

                p("microprofile-jwt-api", "mp.jwt.token.header", "java.lang.String",
                        "Configuration property to specify the `HTTP` header name expected to contain the JWT token.",
                        true, null, null, null, 0, null),

                p("microprofile-jwt-api", "mp.jwt.token.cookie", "java.lang.String",
                        "Configuration property to specify the Cookie name (default is `Bearer`) expected to contain the JWT token. This configuration will be ignored unless `mp.jwt.token.header` is set to `Cookie`.",
                        true, null, null, null, 0, null),

                p("microprofile-jwt-api", "mp.jwt.verify.audiences", "java.lang.String",
                        "Configuration property to specify the list of allowable value(s) for the `aud` claim, separated by commas. If specified, MP-JWT claim must be present and match one of the values.",
                        true, null, null, null, 0, null)
        );
    }
}
