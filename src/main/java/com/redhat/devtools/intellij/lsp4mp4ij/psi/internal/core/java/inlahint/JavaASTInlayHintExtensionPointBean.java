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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.java.inlahint;


import com.intellij.openapi.extensions.PluginAware;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.util.xmlb.annotations.Attribute;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.inlayhint.JavaASTInlayHint;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidator;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

public class JavaASTInlayHintExtensionPointBean implements PluginAware {
    private PluginDescriptor pluginDescriptor;

    @Attribute
    public String implementation;

    @Override
    public void setPluginDescriptor(@NotNull PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
    }

    public JavaASTInlayHint createInlayHint() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return (JavaASTInlayHint) pluginDescriptor.getPluginClassLoader()
                .loadClass(implementation)
                .getConstructor(new Class[0]).newInstance(new Object[0]);
    }
}
