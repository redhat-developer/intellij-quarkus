/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
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
package com.redhat.microprofile.psi.internal.quarkus.builditems.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.symbols.IJavaWorkspaceSymbolsParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.IJaxRsInfoProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsMethodInfo;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.jaxrs.java.JaxRsInfoProviderRegistry;
import com.redhat.devtools.intellij.quarkus.psi.internal.builditems.QuarkusBuildItemUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collects workspace symbols for Quarkus BuildItem.
 */
public class BuildItemWorkspaceSymbolParticipant implements IJavaWorkspaceSymbolsParticipant {

	private static final Logger LOGGER = Logger.getLogger(BuildItemWorkspaceSymbolParticipant.class.getName());

	@Override
	public void collectSymbols(Module project, IPsiUtils utils, List<SymbolInformation> symbols, ProgressIndicator monitor) {
		if (monitor.isCanceled()) {
			return;
		}

		QuarkusBuildItemUtils.getAllBuildItemClasses(project, monitor)
				.forEach(buildType -> {
					symbols.add(createSymbol(buildType, utils ));
				});
	}

	private static SymbolInformation createSymbol(PsiClass buildItemType, IPsiUtils utils) {
		Location location = utils.toLocation(buildItemType);

		StringBuilder nameBuilder = new StringBuilder("&");
		nameBuilder.append(buildItemType.getName());

		SymbolInformation symbol = new SymbolInformation();
		symbol.setName(nameBuilder.toString());
		symbol.setKind(SymbolKind.Class);
		symbol.setLocation(location);
		return symbol;
	}

}