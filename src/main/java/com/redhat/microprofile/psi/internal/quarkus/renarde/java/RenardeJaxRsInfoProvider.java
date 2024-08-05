/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.renarde.java;


import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.*;
import com.intellij.util.KeyedLazyInstanceEP;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.*;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.hasAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.overlaps;

/**
 * Use custom logic for all JAX-RS features in classes that extends Renarde's
 * <code>Controller</code> class.
 */
public class RenardeJaxRsInfoProvider extends KeyedLazyInstanceEP<IJaxRsInfoProvider> implements IJaxRsInfoProvider {

	private static final Logger LOGGER = Logger.getLogger(RenardeJaxRsInfoProvider.class.getName());

	@Override
	public boolean canProvideJaxRsMethodInfoForClass(@NotNull PsiFile typeRoot,
													 @NotNull Module javaProject,
													 @NotNull ProgressIndicator monitor) {
		return RenardeUtils.isControllerClass(javaProject, typeRoot, monitor);
	}

	@Override
	public @NotNull Set<PsiClass> getAllJaxRsClasses(@NotNull Module javaProject,
											@NotNull IPsiUtils utils,
											@NotNull ProgressIndicator monitor) {
		return RenardeUtils.getAllControllerClasses(javaProject, monitor);
	}

	@Override
	public @NotNull List<JaxRsMethodInfo> getJaxRsMethodInfo(@NotNull PsiFile typeRoot,
															 @NotNull JaxRsContext jaxrsContext,
															 @NotNull IPsiUtils utils,
															 @NotNull ProgressIndicator monitor) {
		try {
			PsiClass type = findFirstClass(typeRoot);
			if (type == null) {
				return Collections.emptyList();
			}
			String pathSegment = JaxRsUtils.getJaxRsPathValue(type);
			String typeSegment = type.getName();

			List<JaxRsMethodInfo> methodInfos = new ArrayList<>();
			for (PsiMethod method : type.getMethods()) {

				if (method.isConstructor() || utils.isHiddenGeneratedElement(method)) {
					continue;
				}
				// ignore element if method range overlaps the type range,
				// happens for generated
				// bytecode, i.e. with lombok
				if (overlaps(type.getNameIdentifier().getTextRange(), method.getNameIdentifier().getTextRange())) {
					continue;
				}

				if (method.getModifierList().hasExplicitModifier(PsiModifier.PUBLIC)) {

					String methodSegment = JaxRsUtils.getJaxRsPathValue(method);
					if (methodSegment == null) {
						methodSegment = method.getName();
					}
					String path;
					if (pathSegment == null) {
						path = methodSegment.startsWith("/") ? methodSegment : JaxRsUtils.buildURL(typeSegment, methodSegment);
					} else {
						path = JaxRsUtils.buildURL(pathSegment, methodSegment);
					}
					String url = JaxRsUtils.buildURL(jaxrsContext.getLocalBaseURL(), path);
					
					JaxRsMethodInfo methodInfo = createMethodInfo(method, url);
					if (methodInfo != null) {
						methodInfos.add(methodInfo);
					}
				}
			}
			return methodInfos;
		} catch (ProcessCanceledException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while collecting JAX-RS methods for Renarde", e);
			return Collections.emptyList();
		}
	}

	private PsiClass findFirstClass(PsiFile typeRoot) {
		for (PsiElement element:typeRoot.getChildren()) {
			if (element instanceof PsiClass) {
				return (PsiClass) element;
			}
		}
		return null;
	}

	private static JaxRsMethodInfo createMethodInfo(PsiMethod method, String url) {

		PsiFile resource = method.getContainingFile();
		if (resource == null) {
			return null;
		}
		String documentUri = LSPIJUtils.toUriAsString(resource);

		HttpMethod httpMethod = HttpMethod.GET;
		for (String methodAnnotationFQN : JaxRsConstants.HTTP_METHOD_ANNOTATIONS) {
			if (hasAnnotation(method, methodAnnotationFQN)) {
				httpMethod = JaxRsUtils.getHttpMethodForAnnotation(methodAnnotationFQN);
				break;
			}
		}

		return new JaxRsMethodInfo(url, httpMethod, method, documentUri);
	}

}