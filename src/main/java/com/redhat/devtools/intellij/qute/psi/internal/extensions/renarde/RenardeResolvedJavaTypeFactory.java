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
package com.redhat.devtools.intellij.qute.psi.internal.extensions.renarde;

import java.util.HashMap;
import java.util.Map;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.*;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ITypeResolver;
import com.redhat.devtools.intellij.qute.psi.internal.template.resolvedtype.AbstractResolvedJavaTypeFactory;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;
import com.redhat.qute.commons.InvalidMethodReason;
import com.redhat.qute.commons.JavaMethodInfo;
import com.redhat.qute.commons.ResolvedJavaTypeInfo;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverKind;
import com.redhat.qute.commons.jaxrs.JaxRsMethodKind;
import com.redhat.qute.commons.jaxrs.JaxRsParamKind;
import com.redhat.qute.commons.jaxrs.RestParam;
import org.gradle.internal.impldep.org.testng.annotations.IAnnotation;

/**
 * Custom Factory to create an {@link ResolvedJavaTypeInfo} instance for Renarde
 * controller.
 * 
 * @author Angelo ZERR
 *
 */
public class RenardeResolvedJavaTypeFactory extends AbstractResolvedJavaTypeFactory {

	private static final String JAVAX_WS_RS_POST_ANNOTATION = "javax.ws.rs.POST";
	private static final String JAKARTA_WS_RS_POST_ANNOTATION = "jakarta.ws.rs.POST";

	private static final String REST_FORM_ANNOTATION = "org.jboss.resteasy.reactive.RestForm";

	private static final String JAVAX_WS_RS_FORM_PARAM_ANNOTATION = "javax.ws.rs.FormParam";

	private static final String JAKARTA_WS_RS_FORM_PARAM_ANNOTATION = "jakarta.ws.rs.FormParam";


	private static final String JAVAX_VALIDATION_CONSTRAINTS_NOTBLANK_ANNOTATION = "javax.validation.constraints.NotBlank";

	private static final String JAKARTA_VALIDATION_CONSTRAINTS_NOTBLANK_ANNOTATION = "jakarta.validation.constraints.NotBlank";

	private static final String REST_PATH_ANNOTATION = "org.jboss.resteasy.reactive.RestPath";

	private static final String JAVAX_WS_RS_PATH_PARAM_ANNOTATION = "javax.ws.rs.PathParam";

	private static final String JAKARTA_WS_RS_PATH_PARAM_ANNOTATION = "jakarta.ws.rs.PathParam";

	private static final String REST_QUERY_ANNOTATION = "org.jboss.resteasy.reactive.RestQuery";

	private static final String JAVAX_WS_RS_QUERY_PARAM_ANNOTATION = "javax.ws.rs.QueryParam";

	private static final String JAKARTA_WS_RS_QUERY_PARAM_ANNOTATION = "jakarta.ws.rs.QueryParam";

	@Override
	public boolean isAdaptedFor(ValueResolverKind kind) {
		return kind == ValueResolverKind.Renarde;
	}

	@Override
	protected boolean isValidField(PsiField field, PsiClass type)  {
		return false;
	}

	@Override
	protected boolean isValidRecordField(PsiRecordComponent field, PsiClass type) {
		return false;
	}

	@Override
	protected InvalidMethodReason getValidMethodForQute(PsiMethod method, String typeName) {
		return null;
	}

	@Override
	protected JavaMethodInfo createMethod(PsiMethod method, ITypeResolver typeResolver) {
		JavaMethodInfo info = super.createMethod(method, typeResolver);
		collectJaxrsInfo(method, info);
		return info;
	}

	private static void collectJaxrsInfo(PsiMethod method, JavaMethodInfo info) {
		// By default all public methods are GET
		JaxRsMethodKind methodKind = JaxRsMethodKind.GET;
		// TODO : we support only @POST, we need to support @PUT, @DELETE, when we will need it.
		if (isPostMethod(method)) {
			methodKind = JaxRsMethodKind.POST;
		}
		info.setJaxRsMethodKind(methodKind);
		try {
			Map<String, RestParam> restParameters = null;
			PsiParameter[] parameters = method.getParameterList().getParameters();
			for (PsiParameter parameter : parameters) {
				// @RestForm, @FormParam
				PsiAnnotation formAnnotation = AnnotationUtils.getAnnotation(parameter, REST_FORM_ANNOTATION,
						JAVAX_WS_RS_FORM_PARAM_ANNOTATION, JAKARTA_WS_RS_FORM_PARAM_ANNOTATION);
				if (formAnnotation != null) {
					if (restParameters == null) {
						restParameters = new HashMap<>();
					}
					PsiAnnotation notBlankAnnotation = AnnotationUtils.getAnnotation(parameter,
							JAVAX_VALIDATION_CONSTRAINTS_NOTBLANK_ANNOTATION,
							JAKARTA_VALIDATION_CONSTRAINTS_NOTBLANK_ANNOTATION);
					boolean required = notBlankAnnotation != null;
					fillRestParam(parameter, formAnnotation, JaxRsParamKind.FORM, restParameters, required);
				} else {
					// @RestPath, @PathParam
					PsiAnnotation pathAnnotation = AnnotationUtils.getAnnotation(parameter, REST_PATH_ANNOTATION,
							JAVAX_WS_RS_PATH_PARAM_ANNOTATION, JAKARTA_WS_RS_PATH_PARAM_ANNOTATION);
					if (pathAnnotation != null) {
						if (restParameters == null) {
							restParameters = new HashMap<>();
						}
						fillRestParam(parameter, pathAnnotation, JaxRsParamKind.PATH, restParameters);
					} else {
						// @RestQuery, @QueryParam
						PsiAnnotation queryAnnotation = AnnotationUtils.getAnnotation(parameter, REST_QUERY_ANNOTATION,
								JAVAX_WS_RS_QUERY_PARAM_ANNOTATION, JAKARTA_WS_RS_QUERY_PARAM_ANNOTATION);
						if (queryAnnotation != null) {
							if (restParameters == null) {
								restParameters = new HashMap<>();
							}
							fillRestParam(parameter, queryAnnotation, JaxRsParamKind.QUERY, restParameters);
						}
					}
				}
			}
			if (restParameters != null) {
				info.setRestParameters(restParameters);
			}
		} catch (Exception e) {

		}
	}

	private static void fillRestParam(PsiParameter parameter, PsiAnnotation formAnnotation,
									  JaxRsParamKind parameterKind, Map<String, RestParam> restParameters) {
		fillRestParam(parameter, formAnnotation, parameterKind, restParameters, false);
	}

	private static void fillRestParam(PsiParameter parameter, PsiAnnotation formAnnotation,
									  JaxRsParamKind parameterKind, Map<String, RestParam> restParameters, boolean required) {
		String parameterName = parameter.getName();
		String formName = parameterName;
		String value = AnnotationUtils.getAnnotationMemberValue(formAnnotation, "value");
		if (value != null) {
			formName = value;
		}
		restParameters.put(parameterName, new RestParam(formName, parameterKind, required));
	}

	private static boolean isPostMethod(PsiMethod method) {
			return AnnotationUtils.hasAnnotation(method, JAVAX_WS_RS_POST_ANNOTATION, JAKARTA_WS_RS_POST_ANNOTATION);
	}

}
