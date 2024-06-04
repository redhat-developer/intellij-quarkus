/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens.IJavaCodeLensParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens.JavaCodeLensContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.HttpMethod;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.IJaxRsInfoProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsMethodInfo;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsUtils.createURLCodeLens;

/**
 *
 * JAX-RS CodeLens participant
 *
 * @author Angelo ZERR
 *
 */
public class JaxRsCodeLensParticipant implements IJavaCodeLensParticipant {

	private static final Logger LOGGER = Logger.getLogger(JaxRsCodeLensParticipant.class.getName());

	private static final String LOCALHOST = "localhost";

	private static final int PING_TIMEOUT = 2000;

	private static final String JAX_RS_INFO_PROVIDER = IJaxRsInfoProvider.class.getName();
	@Override
	public boolean isAdaptedForCodeLens(JavaCodeLensContext context, ProgressIndicator monitor) {
		MicroProfileJavaCodeLensParams params = context.getParams();
		if (!params.isUrlCodeLensEnabled()) {
			return false;
		}
		PsiFile typeRoot = context.getTypeRoot();
		Module project = context.getJavaProject();
		IJaxRsInfoProvider jaxRsProvider= JaxRsInfoProviderRegistry.getInstance().getProviderForType(typeRoot, project, monitor);
		if (jaxRsProvider != null) {
			context.put(JAX_RS_INFO_PROVIDER, jaxRsProvider);
		}
		// if some jaxrs info provider can provide jaxrs method info for this class, provide lens
		return jaxRsProvider != null;
	}

	@Override
	public void beginCodeLens(JavaCodeLensContext context, ProgressIndicator monitor) {
		JaxRsContext.getJaxRsContext(context).getApplicationPath();
	}

	@Override
	public List<CodeLens> collectCodeLens(JavaCodeLensContext context, ProgressIndicator monitor)  {
		PsiFile typeRoot = context.getTypeRoot();
		JaxRsContext jaxrsContext = JaxRsContext.getJaxRsContext(context);
		IPsiUtils utils = context.getUtils();

		if (context.getParams().isCheckServerAvailable()
				&& !isServerAvailable(LOCALHOST, jaxrsContext.getServerPort(), PING_TIMEOUT)) {
			return Collections.emptyList();
		}

		IJaxRsInfoProvider provider = (IJaxRsInfoProvider) context.get(JAX_RS_INFO_PROVIDER);
		if (provider == null) {
			return Collections.emptyList();
		}
		List<JaxRsMethodInfo> infos = provider.getJaxRsMethodInfo(typeRoot, jaxrsContext, utils, monitor);

		MicroProfileJavaCodeLensParams params = context.getParams();
		return infos.stream() //
				.map(methodInfo -> {
					try {
						return createCodeLens(methodInfo, params.getOpenURICommand(), utils);
					} catch (ProcessCanceledException e) {
						//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
						//TODO delete block when minimum required version is 2024.2
						throw e;
					} catch (IndexNotReadyException | CancellationException e) {
						throw e;
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "failed to create codelens for jax-rs method", e);
						return null;
					}
				}) //
				.filter(lens -> lens != null) //
				.collect(Collectors.toList());
	}

	/**
	 * Returns a code lens for the given JAX-RS method information.
	 *
	 * @param methodInfo       the JAX-RS method information to build the code lens
	 *                         out of
	 * @param openUriCommandId the id of the client command to invoke to open a URL
	 *                         in the browser
	 * @param utils            the jdt utils
	 * @return a code lens for the given JAX-RS method information
	 */
	private static CodeLens createCodeLens(JaxRsMethodInfo methodInfo, String openUriCommandId, IPsiUtils utils) {
		PsiMethod method = methodInfo.getJavaMethod();
		CodeLens lens = createURLCodeLens(method, utils, false);
		if(lens == null) {
			return null;
		}
		lens.setCommand(new Command(methodInfo.getUrl(), //
				isHttpMethodClickable(methodInfo.getHttpMethod()) && openUriCommandId != null ? openUriCommandId : "", //
				Collections.singletonList(methodInfo.getUrl())));
		return lens;
	}

	private static boolean isHttpMethodClickable(HttpMethod httpMethod) {
		return HttpMethod.GET.equals(httpMethod);
	}

	private static boolean isServerAvailable(String host, int port, int timeout) {
		/*try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), timeout);
			return true;
		} catch (IOException e) {
			return false;
		}*/
		// As IJ InlayHints cannot be computed in async mode with a CompletableFuture
		// we consider that the server is every time available
		// We do that because the check with Socket takes some times and freeze the Java Editor
		// as soon as the user type something in the Java Editor.
		// By returning true, the URL codelens will be displayed even if the server is not started.
		return true;
	}

}
