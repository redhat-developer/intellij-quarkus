/*******************************************************************************
* Copyright (c) 2024, 2025 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.jaxrs.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.symbols.IJavaWorkspaceSymbolsParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.IJaxRsInfoProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsMethodInfo;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collects workspace symbols for JAX-RS REST endpoints.
 */
public class JaxRsWorkspaceSymbolParticipant implements IJavaWorkspaceSymbolsParticipant {

	private static final Logger LOGGER = Logger.getLogger(JaxRsWorkspaceSymbolParticipant.class.getName());

	@Override
	public void collectSymbols(Module project, IPsiUtils utils, List<SymbolInformation> symbols, ProgressIndicator monitor) {
		if (monitor.isCanceled()) {
			return;
		}

		JaxRsContext jaxrsContext = new JaxRsContext(project);
		Set<PsiClass> jaxrsTypes = getAllJaxRsTypes(project, utils, monitor);
		if (jaxrsTypes == null || monitor.isCanceled()) {
			return;
		}
		List<JaxRsMethodInfo> methodsInfo = new ArrayList<>();
		for (PsiClass typeRoot : jaxrsTypes) {
			IJaxRsInfoProvider provider = getProviderForType(typeRoot, project, monitor);
			if (provider != null) {
				methodsInfo.addAll(provider.getJaxRsMethodInfo(typeRoot.getContainingFile(), jaxrsContext, utils, monitor));
			}
			if (monitor.isCanceled()) {
				return;
			}
		}

		methodsInfo.forEach(methodInfo -> {
			try {
				symbols.add(createSymbol(methodInfo, utils));
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "failed to create workspace symbol for jax-rs method", e);
			}
		});
	}

	/**
	 * Returns the provider that can provide JAX-RS method info for the given class,
	 * or null if no provider can provide info.
	 *
	 * @param typeRoot the class to collect JAX-RS method info for
	 * @param project the Module project
	 * @param monitor the progress monitor
	 * @return the provider that can provide JAX-RS method info for the given class,
	 *         or null if no provider can provide info
	 */
	private static IJaxRsInfoProvider getProviderForType(PsiClass typeRoot, Module project,  ProgressIndicator monitor) {
		for (IJaxRsInfoProvider provider : JaxRsInfoProviderRegistry.getInstance().getProviders()) {
			if (provider.canProvideJaxRsMethodInfoForClass(typeRoot.getContainingFile(), project, monitor)) {
				return provider;
			}
		}
		LOGGER.severe("Attempted to collect JAX-RS info for " + typeRoot.getQualifiedName()
				+ ", but no participant was suitable, despite the fact that an earlier check found a suitable participant");
		return null;
	}

	private static Set<PsiClass> getAllJaxRsTypes(Module javaProject, IPsiUtils utils, ProgressIndicator monitor) {
		Set<PsiClass> jaxrsTypes = new HashSet<>();
		for (IJaxRsInfoProvider provider : JaxRsInfoProviderRegistry.getInstance().getProviders()) {
			jaxrsTypes.addAll(provider.getAllJaxRsClasses(javaProject, utils,monitor));
			if (monitor.isCanceled()) {
				return null;
			}
		}
		return jaxrsTypes;
	}

	private static SymbolInformation createSymbol(JaxRsMethodInfo methodInfo, IPsiUtils utils) throws MalformedURLException {
		TextRange sourceRange = methodInfo.getJavaMethod().getNameIdentifier().getTextRange();
		Range r = utils.toRange(methodInfo.getJavaMethod(), sourceRange.getStartOffset(), sourceRange.getLength());
		Location location = new Location(methodInfo.getDocumentUri(), r);

		StringBuilder nameBuilder = new StringBuilder("@");
		URL url;
		try {
			url = new URI(methodInfo.getUrl()).toURL();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		String path = url.getPath();
		nameBuilder.append(path);
		nameBuilder.append(": ");
		nameBuilder.append(methodInfo.getHttpMethod());

		SymbolInformation symbol = new SymbolInformation();
		symbol.setName(nameBuilder.toString());
		symbol.setKind(SymbolKind.Method);
		symbol.setLocation(location);
		return symbol;
	}

}