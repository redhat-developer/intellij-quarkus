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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.microprofile.commons.ClasspathKind;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

/**
 * {@link IPsiUtils} implementation.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/ls/JDTUtilsLSImpl.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/ls/JDTUtilsLSImpl.java</a>
 */
public class PsiUtils implements IPsiUtils {
    private static final IPsiUtils INSTANCE = new PsiUtils();

    public static IPsiUtils getInstance() {
        return INSTANCE;
    }

    private PsiUtils() {
    }

    @Override
    public VirtualFile findFile(String uri) throws URISyntaxException {
        return LocalFileSystem.getInstance().findFileByIoFile(Paths.get(new URI(uri)).toFile());
    }

    public static ClasspathKind getClasspathKind(VirtualFile file, Module module) {
        return ModuleRootManager.getInstance(module).getFileIndex().isInTestSourceContent(file)?ClasspathKind.TEST:ClasspathKind.SRC;
    }

    public static String getProjectURI(Module module) {
        return module.getModuleFilePath();
    }
}
