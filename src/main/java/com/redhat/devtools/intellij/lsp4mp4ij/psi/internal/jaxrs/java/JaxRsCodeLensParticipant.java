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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens.IJavaCodeLensParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens.JavaCodeLensContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsUtils.createURLCodeLens;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsUtils.getJaxRsPathValue;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsUtils.isJaxRsRequestMethod;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.overlaps;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.jaxrs.JaxRsConstants.JAVAX_WS_RS_PATH_ANNOTATION;

/**
 *
 * JAX-RS CodeLens participant
 *
 * @author Angelo ZERR
 *
 */
public class JaxRsCodeLensParticipant implements IJavaCodeLensParticipant {

	private static final String LOCALHOST = "localhost";

	private static final int PING_TIMEOUT = 2000;

	@Override
	public boolean isAdaptedForCodeLens(JavaCodeLensContext context) {
		MicroProfileJavaCodeLensParams params = context.getParams();
		if (!params.isUrlCodeLensEnabled()) {
			return false;
		}
		// Collection of URL codeLens is done only if JAX-RS is on the classpath
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, JAVAX_WS_RS_PATH_ANNOTATION) != null;
	}

	@Override
	public List<CodeLens> collectCodeLens(JavaCodeLensContext context) {
		PsiFile typeRoot = context.getTypeRoot();
		PsiElement[] elements = typeRoot.getChildren();
		int serverPort = JaxRsContext.getJaxRsContext(context).getServerPort();
		IPsiUtils utils = context.getUtils();
		MicroProfileJavaCodeLensParams params = context.getParams();
		params.setLocalServerPort(serverPort);
		List<CodeLens> lenses = new ArrayList<>();
		collectURLCodeLenses(elements, null, lenses, params, utils);
		return lenses;
	}

	private static void collectURLCodeLenses(PsiElement[] elements, String rootPath, Collection<CodeLens> lenses,
			MicroProfileJavaCodeLensParams params, IPsiUtils utils) {
		for (PsiElement element : elements) {
			if (element instanceof PsiClass) {
				PsiClass type = (PsiClass) element;
				// Get value of JAX-RS @Path annotation from the class
				String pathValue = getJaxRsPathValue(type);
				if (pathValue != null) {
					// Class is annotated with @Path
					// Display code lens only if local server is available.
					if (!params.isCheckServerAvailable()
							|| isServerAvailable(LOCALHOST, params.getLocalServerPort(), PING_TIMEOUT)) {
						// Loop for each method annotated with @Path to generate URL code lens per
						// method.
						collectURLCodeLenses(type.getChildren(), pathValue, lenses, params, utils);
					}
				}
				continue;
			} else if (element instanceof PsiMethod) {
				if (utils.isHiddenGeneratedElement(element)) {
					continue;
				}
				// ignore element if method range overlaps the type range, happens for generated
				// bytecode, i.e. with lombok
				PsiClass parentType = PsiTreeUtil.getParentOfType(element, PsiClass.class);
				if (parentType != null && overlaps(parentType.getNameIdentifier().getTextRange(),
						((PsiMethod) element).getNameIdentifier().getTextRange())) {
					continue;
				}
			} else {// neither a type nor a method, we bail
				continue;
			}

			// Here java element is a method
			if (rootPath != null) {
				PsiMethod method = (PsiMethod) element;
				// A JAX-RS method is a public method annotated with @GET @POST, @DELETE, @PUT
				// JAX-RS
				// annotation
				if (isJaxRsRequestMethod(method) && method.getModifierList().hasExplicitModifier(PsiModifier.PUBLIC)) {
					String baseURL = params.getLocalBaseURL();
					String openURICommandId = params.getOpenURICommand();
					CodeLens lens = createURLCodeLens(baseURL, rootPath, openURICommandId, (PsiMethod) element, utils);
					if (lens != null) {
						lenses.add(lens);
					}
				}
			}
		}
	}

	private static boolean isServerAvailable(String host, int port, int timeout) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), timeout);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
