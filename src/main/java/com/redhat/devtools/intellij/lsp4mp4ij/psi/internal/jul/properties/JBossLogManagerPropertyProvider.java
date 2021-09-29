/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.jul.properties;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractStaticPropertiesProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.IPropertiesCollector.MergingStrategy;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;

import java.util.logging.Level;

/**
 * JBoss Logging Manager provider for {@link Level}.
 * 
 * @author Angelo ZERR
 * 
 * @see <a href="https://github.com/jboss-logging/jboss-logmanager/blob/master/core/src/main/java/org/jboss/logmanager/Level.java">https://github.com/jboss-logging/jboss-logmanager/blob/master/core/src/main/java/org/jboss/logmanager/Level.java</a>
 * @see <a href="https://github.com/eclipse/lsp4mp/blob/master/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/internal/jul/properties/JBossLogManagerPropertyProvider.java">https://github.com/eclipse/lsp4mp/blob/master/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/internal/jul/properties/JBossLogManagerPropertyProvider.java</a>
 *
 */
public class JBossLogManagerPropertyProvider extends AbstractStaticPropertiesProvider {

	private static final String JBOSS_LOGMANAGER_LEVEL_CLASS = "org.jboss.logmanager.Level";

	public JBossLogManagerPropertyProvider() {
		super("/static-properties/jboss-logmanager-metadata.json",
				MergingStrategy.REPLACE /* JUL 'INFO' must be overrided with JBossLogManager 'INFO' */);
	}

	@Override
	protected boolean isAdaptedFor(SearchContext context) {
		// Check if JBoss LogManager exists in classpath
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, JBOSS_LOGMANAGER_LEVEL_CLASS) != null;
	}
}
