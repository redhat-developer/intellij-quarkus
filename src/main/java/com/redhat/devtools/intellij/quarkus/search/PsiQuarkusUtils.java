/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;

/**
 * JDT Quarkus utilities.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.quarkus/src/main/java/com/redhat/microprofile/jdt/internal/quarkus/JDTQuarkusUtils.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.quarkus/src/main/java/com/redhat/microprofile/jdt/internal/quarkus/JDTQuarkusUtils.java</a>
 *
 */
public class PsiQuarkusUtils {
    public static String getExtensionName(String location) {
        if (location == null) {
            return null;
        }
        if (!location.endsWith(".jar")) {
            return null;
        }
        int start = location.lastIndexOf('/');
        start++;
        int end = location.lastIndexOf('-');
        if (end == -1) {
            end = location.lastIndexOf('.');
        }
        if (end < start) {
            return null;
        }
        String extensionName = location.substring(start, end);
        if (extensionName.endsWith("-deployment")) {
            extensionName = extensionName.substring(0, extensionName.length() - "-deployment".length());
        }
        return extensionName;
    }
}
