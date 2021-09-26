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

import java.util.logging.Level;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractStaticPropertiesProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;

/**
 * Java Util Logging properties provider for {@link Level}.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/eclipse/lsp4mp/blob/master/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/internal/jul/properties/JULPropertyProvider.java">https://github.com/eclipse/lsp4mp/blob/master/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/internal/jul/properties/JULPropertyProvider.java</a>
 *
 */
public class JULPropertyProvider extends AbstractStaticPropertiesProvider {

	public JULPropertyProvider() {
		super("/static-properties/jul-metadata.json");
	}

	@Override
	protected boolean isAdaptedFor(SearchContext context) {
		return true;
	}
}
