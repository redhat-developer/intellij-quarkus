/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.config.java;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.LSP4MPMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileMavenProjectName;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.providers.MicroProfileConfigSourceProvider;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

/**
 * MicroProfileConfig name definition to properties files.
 */
public class MicroProfileConfigJavaDefinitionTest extends LSP4MPMavenModuleImportingTestCase {

    @Test
    public void testConfigPropertyNameDefinition() throws Exception {


        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_hover);

        String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingResource.java", javaProject);
        String propertiesFileUri = getFileUri("src/main/resources/META-INF/microprofile-config.properties", javaProject);

        saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE, //
                "greeting.message = hello\r\n" + //
                        "greeting.name = quarkus\r\n" + //
                        "greeting.number = 100",
                javaProject);
        // Position(14, 40) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.mes|sage")
        assertJavaDefinitions(p(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), //
                def(r(14, 28, 44), propertiesFileUri, "greeting.message"));

        // Test left edge
        // Position(14, 28) is the character after the | symbol:
        // @ConfigProperty(name = "|greeting.message")
        assertJavaDefinitions(p(14, 28), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), //
                def(r(14, 28, 44), propertiesFileUri, "greeting.message"));

        // Test right edge
        // Position(14, 43) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.messag|e")
        assertJavaDefinitions(p(14, 43), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), //
                def(r(14, 28, 44), propertiesFileUri, "greeting.message"));

        // Test no hover
        // Position(14, 27) is the character after the | symbol:
        // @ConfigProperty(name = |"greeting.message")
        assertJavaDefinitions(p(14, 27), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()));

        // Test no hover 2
        // Position(14, 44) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.message|")
        assertJavaDefinitions(p(14, 44), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()));

        // Definition override default value
        // Position(26, 33) is the character after the | symbol:
        // @ConfigProperty(name = "greet|ing.number", defaultValue="0")
        assertJavaDefinitions(p(29, 33), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), //
                def(r(29, 28, 43), propertiesFileUri, "greeting.number"));

        // Definition when no value
        // Position(23, 33) is the character after the | symbol:
        // @ConfigProperty(name = "greet|ing.missing")
        assertJavaDefinitions(p(23, 33), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()));

    }
}