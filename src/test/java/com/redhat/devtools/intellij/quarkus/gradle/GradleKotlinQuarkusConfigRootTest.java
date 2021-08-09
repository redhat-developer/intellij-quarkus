/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.gradle;

import com.redhat.devtools.intellij.quarkus.search.PropertiesManager;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.p;
import static org.eclipse.lsp4mp.commons.metadata.ItemMetadata.CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED;
import static org.eclipse.lsp4mp.commons.metadata.ItemMetadata.CONFIG_PHASE_BUILD_TIME;
import static org.eclipse.lsp4mp.commons.metadata.ItemMetadata.CONFIG_PHASE_RUN_TIME;

public class GradleKotlinQuarkusConfigRootTest extends GradleTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        FileUtils.copyDirectory(new File("projects/gradle/kotlin-resteasy"), new File(getProjectPath()));
        importProject();
    }

    @Test
    public void testAllExtensions() throws Exception {
        MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(getModule("kotlin-resteasy.main"), MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsImpl.getInstance(), DocumentFormat.PlainText);

        assertProperties(info,

                p("quarkus-core", "quarkus.application.name",
                        "java.util.Optional<java.lang.String>", "The name of the application.\nIf not set, defaults to the name of the project (except for tests where it is not set at all).", true,
                        "io.quarkus.runtime.ApplicationConfig",
                        "name", null, CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED, null));

    }
}
