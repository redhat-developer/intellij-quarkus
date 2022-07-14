/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.intellij.quarkus.utils;

/**
 * Build tool enumeration
 *
 * @author zcervink@redhat.com
 */
public enum BuildTool {
    MAVEN("Maven"),
    GRADLE("Gradle");

    private String textRepresentation;

    BuildTool(String textRepresentation) {
        this.textRepresentation = textRepresentation;
    }

    @Override
    public String toString() {
        return this.textRepresentation;
    }
}
