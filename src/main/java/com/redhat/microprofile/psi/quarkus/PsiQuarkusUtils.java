/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.microprofile.psi.quarkus;

import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.intellij.psi.search.GlobalSearchScope;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import org.eclipse.lsp4mp.commons.metadata.ConverterKind;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;

import java.util.Arrays;
import java.util.List;

/**
 * JDT Quarkus utilities.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.quarkus/src/main/java/com/redhat/microprofile/jdt/internal/quarkus/JDTQuarkusUtils.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.quarkus/src/main/java/com/redhat/microprofile/jdt/internal/quarkus/JDTQuarkusUtils.java</a>
 *
 */
public class PsiQuarkusUtils {
    private static final List<ConverterKind> DEFAULT_QUARKUS_CONVERTERS = Arrays.asList(ConverterKind.KEBAB_CASE,
            ConverterKind.VERBATIM);

    public static String getExtensionName(String location) {
        if (location == null) {
            return null;
        }
        if (location.endsWith("!/")) {
            // In IJ, location JAR ends with !/
            location = location.substring(0, location.length() - 2);
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

    public static void updateConverterKinds(ItemMetadata metadata, PsiMember member, PsiClass enclosedType) {
        if (enclosedType == null || !enclosedType.isEnum()) {
            return;
        }
        // By default Quarkus set the enum values as kebab and verbatim
        metadata.setConverterKinds(DEFAULT_QUARKUS_CONVERTERS);
    }

    public static boolean isSupportNamingStrategy(Module javaProject) {
        JavaPsiFacade facade = JavaPsiFacade.getInstance(javaProject.getProject());
        return facade.findClass(QuarkusConstants.CONFIG_PROPERTIES_NAMING_STRATEGY_ENUM, GlobalSearchScope.moduleWithLibrariesScope(javaProject)) != null;
    }

    /**
     * Returns true if <code>javaProject</code> is a Quarkus project. Returns false
     * otherwise.
     *
     * @param javaProject the Java project to check
     * @return true only if <code>javaProject</code> is a Quarkus project.
     */
    public static boolean isQuarkusProject(Module javaProject) {
            return PsiUtilsLSImpl.getInstance(javaProject.getProject()).findClass(javaProject, QuarkusConstants.QUARKUS_RUNTIME_CLASS_NAME) != null;
    }

}
