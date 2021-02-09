/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.maven;

import com.intellij.testFramework.fixtures.CodeInsightTestFixture;

public abstract class MavenEditorTest extends MavenImportingTestCase {
    protected CodeInsightTestFixture codeInsightTestFixture;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        codeInsightTestFixture = (CodeInsightTestFixture) myTestFixture;
    }
}
