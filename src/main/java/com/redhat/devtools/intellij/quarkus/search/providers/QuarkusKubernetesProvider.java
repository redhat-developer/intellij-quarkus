/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.providers;


import com.intellij.psi.PsiClass;

import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.quarkus.search.IPropertiesCollector;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.quarkus.search.SearchContext;
import com.redhat.microprofile.commons.DocumentFormat;

import io.quarkus.runtime.util.StringUtil;

import static com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils.getPropertyType;
import static com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils.getSourceMethod;
import static com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils.isSimpleFieldType;

/**
 * Properties provider to collect Quarkus properties from the io dekorate config
 * class for Kubernetes, OpenShift, Docker and S2i and generates the same
 * properties than https://quarkus.io/guides/kubernetes#configuration-options
 * 
 * <p>
 * As Quarkus Kubernetes doesn't use a standard mechanism with
 * Quarkus @ConfigRoot annotation to collect properties. Indeed the
 * io.quarkus.kubernetes.deployment.KubernetesProcessor which manages Quarkus
 * kubernetes.*, openshift.* properties uses the io dekorate project, so we need
 * to introspect the io dekorate config class like
 * io.dekorate.kubernetes.config.KubernetesConfig to generate kubernetes,
 * openshift, docker and s2i properties.
 * </p>
 * 
 * <p>
 * io.dekorate.kubernetes.config.KubernetesConfig i sgenerated from the
 * annotation io.dekorate.kubernetes.annotation.KubernetesApplication. it's
 * better to use this annotation type to get default value and javadoc.
 * </p>
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.quarkus/src/main/java/com/redhat/microprofile/jdt/internal/quarkus/providers/QuarkusKubernetesProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.quarkus/src/main/java/com/redhat/microprofile/jdt/internal/quarkus/providers/QuarkusKubernetesProvider.java</a>
 * @see <a href="https://quarkus.io/guides/kubernetes#configuration-options">https://quarkus.io/guides/kubernetes#configuration-options</a>
 */
public class QuarkusKubernetesProvider extends AbstractTypeDeclarationPropertiesProvider {

	private static final String S2I_PREFIX = "s2i";
	private static final String DOCKER_PREFIX = "docker";
	private static final String OPENSHIFT_PREFIX = "openshift";
	private static final String KUBERNETES_PREFIX = "kubernetes";
	private static final String KUBERNETES_APPLICATION_ANNOTATION = "io.dekorate.kubernetes.annotation.KubernetesApplication";
	private static final String OPENSHIFT_APPLICATION_ANNOTATION = "io.dekorate.openshift.annotation.OpenshiftApplication";
	private static final String S2I_BUILD_ANNOTATION = "io.dekorate.s2i.annotation.S2iBuild";
	private static final String DOCKER_BUILD_ANNOTATION = "io.dekorate.docker.annotation.DockerBuild";

	@Override
	protected String[] getTypeNames() {
		return new String[] { KUBERNETES_APPLICATION_ANNOTATION, DOCKER_BUILD_ANNOTATION,
				OPENSHIFT_APPLICATION_ANNOTATION, S2I_BUILD_ANNOTATION };
	}

	@Override
	protected Query<PsiModifierListOwner> createSearchPattern(SearchContext context, String annotationName) {
		return createAnnotationTypeDeclarationSearchPattern(context, annotationName);
	}

	@Override
	protected void processClass(PsiClass configType, String className, SearchContext context) {
		String configPrefix = getConfigPrefix(className);
		if (configPrefix != null) {
			IPropertiesCollector collector = context.getCollector();
			IPsiUtils utils = context.getUtils();
			DocumentFormat documentFormat = context.getDocumentFormat();
			collectProperties(configPrefix, configType, collector, utils, documentFormat);
			// We need to hard code some properties because KubernetesProcessor does that
			switch (configPrefix) {
			case KUBERNETES_PREFIX:
				// kubernetes.deployment.target
				// see
				// https://github.com/quarkusio/quarkus/blob/44e5e2e3a642d1fa7af9ddea44b6ff8d37e862b8/extensions/kubernetes/deployment/src/main/java/io/quarkus/kubernetes/deployment/KubernetesProcessor.java#L94
				super.addItemMetadata(collector, "kubernetes.deployment.target", "java.lang.String", //
						"To enable the generation of OpenShift resources, you need to include OpenShift in the target platforms: `kubernetes.deployment.target=openshift`."
								+ System.lineSeparator()
								+ "If you need to generate resources for both platforms (vanilla Kubernetes and OpenShift), then you need to include both (coma separated)."
								+ System.lineSeparator() + "`kubernetes.deployment.target=kubernetes, openshift`.",
						null, null, null, KUBERNETES_PREFIX, null, true);
				// kubernetes.registry
				// see
				// https://github.com/quarkusio/quarkus/blob/44e5e2e3a642d1fa7af9ddea44b6ff8d37e862b8/extensions/kubernetes/deployment/src/main/java/io/quarkus/kubernetes/deployment/KubernetesProcessor.java#L103
				super.addItemMetadata(collector, "kubernetes.registry", "java.lang.String", //
						"Specify the docker registry.", null, null, null, null, null, true);
				break;
			case OPENSHIFT_PREFIX:
				// openshift.registry
				super.addItemMetadata(collector, "openshift.registry", "java.lang.String", //
						"Specify the docker registry.", null, null, null, null, null, true);
				break;
			}
		}
	}

	private void collectProperties(String prefix, PsiClass configType, IPropertiesCollector collector, IPsiUtils utils,
			DocumentFormat documentFormat) {
		String sourceType = configType.getQualifiedName();
		PsiMethod[] methods = configType.getMethods();
		for (PsiMethod method : methods) {
			String resultTypeName = PsiTypeUtils.getResolvedResultTypeName(method);
			PsiClass resultTypeClass = PsiTypeUtils.findType(method.getManager(), resultTypeName);
			String methodName = method.getName();
			String propertyName = prefix + "." + StringUtil.hyphenate(methodName);
			boolean isArray = method.getReturnType().getArrayDimensions() > 0;
			if (isArray) {
				propertyName += "[*]";
			}
			if (isSimpleFieldType(resultTypeClass, resultTypeName)) {
				String type = getPropertyType(resultTypeClass, resultTypeName);
				String description = utils.getJavadoc(method, documentFormat);
				String sourceMethod = getSourceMethod(method);
				String defaultValue = PsiTypeUtils.getDefaultValue(method);
				String extensionName = null;
				super.updateHint(collector, resultTypeClass);

				super.addItemMetadata(collector, propertyName, type, description, sourceType, null, sourceMethod,
						defaultValue, extensionName, PsiTypeUtils.isBinary(method));
			} else {
				collectProperties(propertyName, resultTypeClass, collector, utils, documentFormat);
			}
		}
	}

	private static String getConfigPrefix(String configClassName) {
		switch (configClassName) {
		case KUBERNETES_APPLICATION_ANNOTATION:
			return KUBERNETES_PREFIX;
		case OPENSHIFT_APPLICATION_ANNOTATION:
			return OPENSHIFT_PREFIX;
		case DOCKER_BUILD_ANNOTATION:
			return DOCKER_PREFIX;
		case S2I_BUILD_ANNOTATION:
			return S2I_PREFIX;
		default:
			return null;
		}
	}
}
