/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.lang;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

final class InjectedLanguageReflectionUtil {

    private static Method FIND_INJECTION_HOST_METHOD;

    static {
        try {
            Class<?> clazz = Class.forName(
                    "com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtilBase"
            );
            FIND_INJECTION_HOST_METHOD =
                    clazz.getDeclaredMethod("findInjectionHost", VirtualFile.class);
            FIND_INJECTION_HOST_METHOD.setAccessible(true);
        } catch (Throwable t) {
            FIND_INJECTION_HOST_METHOD = null;
        }
    }

    private InjectedLanguageReflectionUtil() {
    }

    /**
     * Reflection-based access to InjectedLanguageUtilBase.findInjectionHost(VirtualFile),
     * avoiding a direct dependency on a deprecated API.
     *
     * @param file injected virtual file
     * @return the injection host PSI element, or {@code null} if unavailable
     */
    public static @Nullable PsiElement findInjectionHost(@Nullable VirtualFile file) {
        if (file == null || FIND_INJECTION_HOST_METHOD == null) {
            return null;
        }
        try {
            return (PsiElement) FIND_INJECTION_HOST_METHOD.invoke(null, file);
        } catch (Throwable t) {
            return null;
        }
    }
}
