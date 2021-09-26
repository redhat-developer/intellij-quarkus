/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jwt;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.GradleTestCase;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.p;

/**
 * Test the availability of the MicroProfile JWT properties
 *
 * @author Kathryn Kodama
 *
 */
public class GradleMicroProfileJWTTest extends GradleTestCase {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		FileUtils.copyDirectory(new File("projects/gradle/microprofile-jwt"), new File(getProjectPath()));
		importProject();
	}

	@Test
	public void testMicroprofileJWT() throws Exception {

		MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(getModule("microprofile-jwt.main"), MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(), DocumentFormat.PlainText);

		assertProperties(infoFromClasspath,
				// confirm properties are being merged with force, should not overwrite properties coming from ConfigProperty provider

				p(null, "mp.jwt.verify.issuer", "java.lang.String", null,
						true, "io.smallrye.jwt.config.JWTAuthContextInfoProvider", "mpJwtIssuer", null, 0, "NONE"),

				p(null, "mp.jwt.verify.publickey.algorithm", "java.util.Optional<io.smallrye.jwt.algorithm.SignatureAlgorithm>",
						null,
						true, "io.smallrye.jwt.config.JWTAuthContextInfoProvider", "mpJwtPublicKeyAlgorithm", null, 0, null),

				p(null, "mp.jwt.decrypt.key.location", "java.lang.String",
						null,
						true, "io.smallrye.jwt.config.JWTAuthContextInfoProvider", "mpJwtDecryptKeyLocation", null, 0, "NONE"),

				p(null, "mp.jwt.token.header", "java.util.Optional<java.lang.String>",
						null,
						true, "io.smallrye.jwt.config.JWTAuthContextInfoProvider", "mpJwtTokenHeader", null, 0, null),

				p(null, "mp.jwt.token.cookie", "java.util.Optional<java.lang.String>",
						null,
						true, "io.smallrye.jwt.config.JWTAuthContextInfoProvider", "mpJwtTokenCookie", null, 0, null),

				p(null, "mp.jwt.verify.audiences", "java.util.Optional<java.util.Set<java.lang.String>>",
						null,
						true, "io.smallrye.jwt.config.JWTAuthContextInfoProvider", "mpJwtVerifyAudiences", null, 0, null)
		);
	}
}
