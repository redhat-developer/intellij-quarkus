/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
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
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.MicroProfileInlayHintTypeSettings;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintSettings;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

public class MicroProfileConfigJavaInlayHintsTest extends LSP4MPMavenModuleImportingTestCase {

    @Test
    public void testDefaultValuesAndConverters() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_quickstart);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaInlayHintParams inlayHintsParams = new MicroProfileJavaInlayHintParams();
        String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingResource.java", javaProject);
        inlayHintsParams.setUri(javaFileUri);

        // Default values + converters
        inlayHintsParams.setSettings(createInlayHintSettings(true, true));

        assertInlayHints(inlayHintsParams, utils, //
                ih(p(17, 11), "BuiltInConverter "), //
                ih(p(20, 11), "BuiltInConverter "), //
                ih(p(23, 21), "BuiltInConverter "), //
                ih(p(25, 50), ", defaultValue=\"PT15M\""), //
                ih(p(26, 13), "StaticMethodConverter "));

        // Only converters
        inlayHintsParams.setSettings(createInlayHintSettings(true, false));

        assertInlayHints(inlayHintsParams, utils, //
                ih(p(17, 11), "BuiltInConverter "), //
                ih(p(20, 11), "BuiltInConverter "), //
                ih(p(23, 21), "BuiltInConverter "), //
                ih(p(26, 13), "StaticMethodConverter "));

        // Only default values
        inlayHintsParams.setSettings(createInlayHintSettings(false, true));

        assertInlayHints(inlayHintsParams, utils, //
                ih(p(25, 50), ", defaultValue=\"PT15M\""));

    }

    private static MicroProfileJavaInlayHintSettings createInlayHintSettings(boolean showConverters,
                                                                             boolean showDefaultValues) {
        MicroProfileJavaInlayHintSettings inlayHintSettings = new MicroProfileJavaInlayHintSettings(ExecutionMode.SAFE);
        MicroProfileInlayHintTypeSettings converterSettings = new MicroProfileInlayHintTypeSettings();
        converterSettings.setEnabled(showConverters);
        inlayHintSettings.setConverters(converterSettings);
        MicroProfileInlayHintTypeSettings defaultValuesSettings = new MicroProfileInlayHintTypeSettings();
        defaultValuesSettings.setEnabled(showDefaultValues);
        inlayHintSettings.setDefaultValues(defaultValuesSettings);
        return inlayHintSettings;
    }

}
