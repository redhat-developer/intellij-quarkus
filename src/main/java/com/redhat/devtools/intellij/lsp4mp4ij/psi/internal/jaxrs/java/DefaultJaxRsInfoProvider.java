/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
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
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.*;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsConstants.JAKARTA_WS_RS_PATH_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsConstants.JAVAX_WS_RS_PATH_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsUtils.getJaxRsPathValue;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsUtils.isJaxRsRequestMethod;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.hasAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.overlaps;


/**
 * Locates JAX-RS methods in a project or class file using the default semantics.
 */
public class DefaultJaxRsInfoProvider implements IJaxRsInfoProvider {

	private static final Logger LOGGER = Logger.getLogger(DefaultJaxRsInfoProvider.class.getName());

	@Override
	public boolean canProvideJaxRsMethodInfoForClass(PsiFile typeRoot, Module javaProject, ProgressIndicator monitor) {
		return PsiTypeUtils.findType(javaProject, JAVAX_WS_RS_PATH_ANNOTATION) != null
				|| PsiTypeUtils.findType(javaProject, JAKARTA_WS_RS_PATH_ANNOTATION) != null;
	}

	@Override
	public Set<PsiClass> getAllJaxRsClasses(Module javaProject, ProgressIndicator monitor) {
		// TODO: implement when LSP4IJ will support workspace symbols
		return Collections.emptySet();
	}

	@Override
	public List<JaxRsMethodInfo> getJaxRsMethodInfo(PsiFile typeRoot, JaxRsContext jaxrsContext, IPsiUtils utils,
													ProgressIndicator monitor) {
		List<JaxRsMethodInfo> methodInfos = new ArrayList<>();
		try {
			collectJaxRsMethodInfo(typeRoot.getChildren(), null, methodInfos, jaxrsContext, utils, monitor);
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "while collecting JAX-RS method info using the default method", e);
		}
		return methodInfos;
	}

	private static void collectJaxRsMethodInfo(PsiElement[] elements, String rootPath,
											   Collection<JaxRsMethodInfo> jaxRsMethodsInfo, JaxRsContext jaxrsContext, IPsiUtils utils,
											   ProgressIndicator monitor) {
		for (PsiElement element : elements) {
			if (monitor.isCanceled()) {
				return;
			}
			if (element instanceof PsiClass) {
				PsiClass type = (PsiClass) element;
				// Get value of JAX-RS @Path annotation from the class
				String pathValue = getJaxRsPathValue(type);
				if (pathValue != null) {
					// Class is annotated with @Path
					// Loop for each method annotated with @Path to generate
					// URL code lens per
					// method.
					collectJaxRsMethodInfo(type.getChildren(), pathValue, jaxRsMethodsInfo, jaxrsContext, utils,
							monitor);
				}
				continue;
			} else if (element instanceof PsiMethod) {
				PsiMethod method = (PsiMethod) element;
				if (method.isConstructor() || utils.isHiddenGeneratedElement(element)) {
					continue;
				}
				// ignore element if method range overlaps the type range,
				// happens for generated
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
				// A JAX-RS method is a public method annotated with @GET @POST,
				// @DELETE, @PUT
				// JAX-RS
				// annotation
				if (isJaxRsRequestMethod(method) && method.getModifierList().hasExplicitModifier(PsiModifier.PUBLIC)) {
					String baseURL = jaxrsContext.getLocalBaseURL();
					JaxRsMethodInfo info = createJaxRsMethodInfo(baseURL, rootPath, method, utils);
					if (info != null) {
						jaxRsMethodsInfo.add(info);
					}
				}
			}
		}
	}

	/**
	 * Returns the JAX-RS method information for the given Java method using the
	 * default JAX-RS semantics.
	 *
	 * @param baseUrl  the base URL.
	 * @param rootPath the JAX-RS path value.
	 * @param method   the method to build the JAX-RS method information out of
	 * @param utils    the jdt utils
	 * @return the JAX-RS method information for the given Java method using the
	 *         default JAX-RS semantics
	 */
	private static JaxRsMethodInfo createJaxRsMethodInfo(String baseUrl, String rootPath, PsiMethod method,
			IPsiUtils utils)  {
		PsiFile resource = method.getContainingFile();
		if (resource == null) {
			return null;
		}
		String documentUri = LSPIJUtils.toUriAsString(resource);

		HttpMethod httpMethod = null;
		for (String methodAnnotationFQN : JaxRsConstants.HTTP_METHOD_ANNOTATIONS) {
			if (hasAnnotation(method, methodAnnotationFQN)) {
				httpMethod = JaxRsUtils.getHttpMethodForAnnotation(methodAnnotationFQN);
				break;
			}
		}
		if (httpMethod == null) {
			return null;
		}

		String pathValue = getJaxRsPathValue(method);
		String url = JaxRsUtils.buildURL(baseUrl, rootPath, pathValue);

		return new JaxRsMethodInfo(url, httpMethod, method, documentUri);
	}

}