/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.buildtool.gradle;

public class GradleGroovyToolDelegate extends AbstractGradleToolDelegate {

    @Override
    String getScriptName() {
        return "build.gradle";
    }

    @Override
    public String getDisplay() {
        return "Gradle";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
