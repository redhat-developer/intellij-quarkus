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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.java;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens.IJavaCodeLensParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens.JavaCodeLensContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsUtils.createURLCodeLens;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsUtils.getJaxRsPathValue;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsUtils.isJaxRsRequestMethod;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValue;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.overlaps;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.MicroProfileRestClientConstants.REGISTER_REST_CLIENT_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.MicroProfileRestClientConstants.REGISTER_REST_CLIENT_ANNOTATION_BASE_URI;

/**
 *
 * MicroProfile RestClient CodeLens participant
 *
 * @author Angelo ZERR
 *
 */
public class MicroProfileRestClientCodeLensParticipant implements IJavaCodeLensParticipant {

	@Override
	public boolean isAdaptedForCodeLens(JavaCodeLensContext context) {
		MicroProfileJavaCodeLensParams params = context.getParams();
		if (!params.isUrlCodeLensEnabled()) {
			return false;
		}
		// Collection of URL codeLens is done only if @ResgisterRestClient annotation is
		// on the classpath
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, REGISTER_REST_CLIENT_ANNOTATION) != null;
	}

	@Override
	public List<CodeLens> collectCodeLens(JavaCodeLensContext context) {
		PsiFile typeRoot = context.getTypeRoot();
		PsiElement[] elements = typeRoot.getChildren();
		IPsiUtils utils = context.getUtils();
		MicroProfileJavaCodeLensParams params = context.getParams();
		List<CodeLens> lenses = new ArrayList<>();
		PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(context.getJavaProject().getProject())
				.getJDTMicroProfileProject(context.getJavaProject());
		collectURLCodeLenses(elements, null, null, mpProject, lenses, params, utils);
		return lenses;
	}

	private static void collectURLCodeLenses(PsiElement[] elements, String baseURL, String rootPath,
			PsiMicroProfileProject mpProject, Collection<CodeLens> lenses, MicroProfileJavaCodeLensParams params,
			IPsiUtils utils) {
		for (PsiElement element : elements) {
			if (element instanceof PsiClass) {
				PsiClass type = (PsiClass) element;
				String url = getBaseURL(type, mpProject);
				if (url != null) {
					// Get value of JAX-RS @Path annotation from the class
					String pathValue = getJaxRsPathValue(type);
					collectURLCodeLenses(type.getChildren(), url, pathValue, mpProject, lenses, params, utils);
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
			if (baseURL != null) {
				PsiMethod method = (PsiMethod) element;
				// A JAX-RS method is a public method annotated with @GET @POST, @DELETE, @PUT
				// JAX-RS
				// annotation
				if (isJaxRsRequestMethod(method)) {
					String openURICommandId = params.getOpenURICommand();
					CodeLens lens = createURLCodeLens(baseURL, rootPath, openURICommandId, (PsiMethod) element, utils);
					if (lens != null) {
						lenses.add(lens);
					}
				}
			}
		}
	}

	/**
	 * Returns the base URL for the given class type and null otherwise.
	 *
	 * @param type      the class type.
	 * @param mpProject the MicroProfile project
	 * @return the base URL for the given class type and null otherwise.
	 */
	private static String getBaseURL(PsiClass type, PsiMicroProfileProject mpProject) {
		PsiAnnotation registerRestClientAnnotation = getAnnotation(type, REGISTER_REST_CLIENT_ANNOTATION);
		if (registerRestClientAnnotation == null) {
			return null;
		}
		// Search base url from the configured property $class/mp-rest/uri
		String baseURIFromConfig = getBaseURIFromConfig(type, mpProject);
		if (baseURIFromConfig != null) {
			return baseURIFromConfig;
		}
		// Search base url from the configured property $class/mp-rest/url
		String baseURLFromConfig = getBaseURLFromConfig(type, mpProject);
		if (baseURLFromConfig != null) {
			return baseURLFromConfig;
		}
		// Search base url from the @RegisterRestClient/baseUri
		String baseURIFromAnnotation = getAnnotationMemberValue(registerRestClientAnnotation,
				REGISTER_REST_CLIENT_ANNOTATION_BASE_URI);
		return baseURIFromAnnotation;
	}

	private static String getBaseURIFromConfig(PsiClass type, PsiMicroProfileProject mpProject) {
		String property = new StringBuilder(type.getQualifiedName()).append("/mp-rest/uri").toString();
		return mpProject.getProperty(property);
	}

	private static String getBaseURLFromConfig(PsiClass type, PsiMicroProfileProject mpProject) {
		String property = new StringBuilder(type.getQualifiedName()).append("/mp-rest/url").toString();
		return mpProject.getProperty(property);
	}
}
