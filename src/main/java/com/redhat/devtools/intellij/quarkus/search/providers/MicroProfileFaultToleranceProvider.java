/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.providers;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMethod;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.redhat.devtools.intellij.quarkus.search.IPropertiesCollector;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.quarkus.search.SearchContext;
import org.eclipse.lsp4mp.commons.DocumentFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.quarkus.search.core.utils.AnnotationUtils.getAnnotation;
import static com.redhat.devtools.intellij.quarkus.search.core.utils.AnnotationUtils.getAnnotationMemberValue;
import static com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils.findType;
import static com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils.getDefaultValue;
import static com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils.getPropertyType;
import static com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils.getResolvedResultTypeName;
import static com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils.getSourceMethod;
import static com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils.getSourceType;
import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileFaultToleranceConstants.ASYNCHRONOUS_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileFaultToleranceConstants.BULKHEAD_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileFaultToleranceConstants.CIRCUITBREAKER_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileFaultToleranceConstants.FALLBACK_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileFaultToleranceConstants.MP_FAULT_TOLERANCE_NONFALLBACK_ENABLED_DESCRIPTION;
import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileFaultToleranceConstants.MP_FAULT_TOLERANCE_NON_FALLBACK_ENABLED;
import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileFaultToleranceConstants.RETRY_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileFaultToleranceConstants.TIMEOUT_ANNOTATION;

/**
 * Properties provider to collect MicroProfile properties from the MicroProfile
 * Fault Tolerance annotations.
 * 
 * @author Angelo ZERR
 * 
 * @see <a href="https://github.com/eclipse/microprofile-fault-tolerance/blob/master/spec/src/main/asciidoc/configuration.asciidoc">https://github.com/eclipse/microprofile-fault-tolerance/blob/master/spec/src/main/asciidoc/configuration.asciidoc</a>
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/faulttolerance/properties/MicroProfileFaultToleranceProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/faulttolerance/properties/MicroProfileFaultToleranceProvider.java</a>
 *
 */
public class MicroProfileFaultToleranceProvider extends AbstractAnnotationTypeReferencePropertiesProvider {

	private static final Logger LOGGER = Logger.getLogger(MicroProfileFaultToleranceProvider.class.getName());

	private static final String MICROPROFILE_FAULT_TOLERANCE_CONTEXT_KEY = MicroProfileFaultToleranceProvider.class
			.getName() + "#MicroProfileFaultToleranceContext";

	private static final String[] ANNOTATION_NAMES = { ASYNCHRONOUS_ANNOTATION, BULKHEAD_ANNOTATION,
			CIRCUITBREAKER_ANNOTATION, FALLBACK_ANNOTATION, RETRY_ANNOTATION, TIMEOUT_ANNOTATION };

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	static class AnnotationInfo {

		private final String name;

		private final String simpleName;

		private final List<AnnotationParameter> parameters;

		public AnnotationInfo(PsiClass annotation, IPsiUtils utils, DocumentFormat documentFormat) {
			this.name = annotation.getQualifiedName();
			this.simpleName = annotation.getName();
			this.parameters = new ArrayList<>();
			PsiMethod[] methods = annotation.getMethods();
			if (methods != null) {
				for (PsiMethod method : methods) {

					if (method instanceof PsiAnnotationMethod) {
						// name
						String name = method.getName();

						// type
						String methodResultTypeName = getResolvedResultTypeName(method);
						PsiClass returnType = findType(method.getManager(), methodResultTypeName);
						String type = getPropertyType(returnType, methodResultTypeName);

						// description
						String description = utils.getJavadoc(method, documentFormat);

						// Method source
						String sourceType = getSourceType(method);
						String sourceMethod = getSourceMethod(method);

						String defaultValue = getDefaultValue(method);
						// Enumerations
						PsiClass enclosedType = PsiTypeUtils.getEnclosedType(returnType, type, method.getManager());

						AnnotationParameter parameter = new AnnotationParameter(name, type, enclosedType, description,
								sourceType, sourceMethod, defaultValue);
						parameters.add(parameter);
					}
				}
			}
			AnnotationParameter parameter = new AnnotationParameter("enabled", "boolean", null, "Enabling the policy",
					name, null, "true");
			parameters.add(parameter);
		}

		public String getName() {
			return name;
		}

		public String getSimpleName() {
			return simpleName;
		}

		public List<AnnotationParameter> getParameters() {
			return parameters;
		}

	}

	static class AnnotationParameter {

		private final String name;
		private final String type;
		private final PsiClass jdtType;
		private final String description;
		private final String sourceType;
		private final String sourceMethod;
		private final String defaultValue;

		public AnnotationParameter(String name, String type, PsiClass jdtType, String description, String sourceType,
				String sourceMethod, String defaultValue) {
			this.name = name;
			this.type = type;
			this.jdtType = jdtType;
			this.description = description;
			this.sourceType = sourceType;
			this.sourceMethod = sourceMethod;
			this.defaultValue = defaultValue;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}

		public String getDescription() {
			return description;
		}

		public String getSourceType() {
			return sourceType;
		}

		public String getSourceMethod() {
			return sourceMethod;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public PsiClass getJDTType() {
			return jdtType;
		}
	}

	static class MicroProfileFaultToleranceContext {

		private final Module javaProject;

		private final IPsiUtils utils;

		private final DocumentFormat documentFormat;

		private final Map<String, AnnotationInfo> cache;

		private final Set<String> processedAnnotations;

		public MicroProfileFaultToleranceContext(Module javaProject, IPsiUtils utils,
				DocumentFormat documentFormat) {
			this.cache = new HashMap<>();
			this.processedAnnotations = new HashSet<>();
			this.javaProject = javaProject;
			this.utils = utils;
			this.documentFormat = documentFormat;
		}

		public AnnotationInfo getAnnotationInfo(String annotation) {
			AnnotationInfo info = cache.get(annotation);
			if (info != null) {
				return info;
			}
			return registerAnnotation(annotation);
		}

		private AnnotationInfo registerAnnotation(String annotationName) {
			PsiClass annotation = PsiTypeUtils.findType(javaProject, annotationName);
			if (annotation == null) {
				return null;
			}
			// Download sources of MicroProfile Fault Tolerance to retrieve the proper
			// Javadoc
			PsiElement classFile = annotation.getNavigationElement();
			if (classFile != null && classFile instanceof PsiClass) {
					annotation = (PsiClass) classFile;
			}
			AnnotationInfo info = new AnnotationInfo(annotation, utils, documentFormat);
			cache.put(info.getName(), info);
			return info;
		}

		/**
		 * Returns true if generation of properties for the given annotation name has
		 * been processed and false otherwise.
		 * 
		 * @param annotationName the MicroProfile Fault Tolerance annotation.
		 * @return true if generation of properties for the given annotation name has
		 *         been processed and false otherwise.
		 */
		public boolean isProcessed(String annotationName) {
			return isProcessed(null, annotationName);
		}

		/**
		 * Return true if generation of properties for the given class name and
		 * annotation name has been processed and false otherwise.
		 * 
		 * @param className      the class name
		 * @param annotationName the MicroProfile Fault Tolerance annotation.
		 * @return true if generation of properties for the given class name and
		 *         annotation name has been processed and false otherwise.
		 */
		public boolean isProcessed(String className, String annotationName) {
			return processedAnnotations.contains(createPrefix(className, null, annotationName));
		}

		public boolean setProcessed(String annotationName) {
			return setProcessed(null, annotationName);
		}

		public boolean setProcessed(String className, String annotationName) {
			return processedAnnotations.add(createPrefix(className, null, annotationName));
		}

		public void addFaultToleranceProperties(IPropertiesCollector collector) {
			// According MP FT sepcification, there are 2 properties:
			// - MP_Fault_Tolerance_NonFallback_Enabled. This property needs to be hard
			// coded
			// - MP_Fault_Tolerance_Metrics_Enabled. This property comes from
			// https://github.com/smallrye/smallrye-fault-tolerance/blob/09901426a7b2228103a706cc58288ebb59934150/implementation/fault-tolerance/src/main/java/io/smallrye/faulttolerance/metrics/MetricsCollectorFactory.java#L30

			if (processedAnnotations.contains(MP_FAULT_TOLERANCE_NON_FALLBACK_ENABLED)) {
				return;
			}
			collector.addItemMetadata(MP_FAULT_TOLERANCE_NON_FALLBACK_ENABLED, "boolean",
					MP_FAULT_TOLERANCE_NONFALLBACK_ENABLED_DESCRIPTION, null, null, null, "false", null, false, 0);
			processedAnnotations.add(MP_FAULT_TOLERANCE_NON_FALLBACK_ENABLED);
		}
	}

	private void collectProperties(IPropertiesCollector collector, AnnotationInfo info, PsiMember annotatedClassOrMethod,
			PsiAnnotation mpftAnnotation, MicroProfileFaultToleranceContext mpftContext) {
		String annotationName = info.getSimpleName();
		String className = null;
		String methodName = null;
		boolean binary = false;
		String sourceType = null;
		String sourceMethod = null;
		if (annotatedClassOrMethod != null) {
			binary = PsiTypeUtils.isBinary(annotatedClassOrMethod);
			if (annotatedClassOrMethod instanceof PsiClass) {
				PsiClass annotatedClass = (PsiClass) annotatedClassOrMethod;
				className = annotatedClass.getQualifiedName();
				// Check if properties has been generated for the <classname><annotation>:
				if (isProcessed(className, annotationName, mpftContext)) {
					return;
				}
				sourceType = getPropertyType(annotatedClass, className);
			} else if (annotatedClassOrMethod instanceof PsiMethod) {
				PsiMethod annotatedMethod = (PsiMethod) annotatedClassOrMethod;
				className = annotatedMethod.getContainingClass().getQualifiedName();
				methodName = annotatedMethod.getName();
				sourceType = getSourceType(annotatedMethod);
				sourceMethod = getSourceMethod(annotatedMethod);
			}
		} else {
			// Check if properties has been generated for the <annotation>:
			if (isProcessed(null, annotationName, mpftContext)) {
				return;
			}
		}

		String prefix = createPrefix(className, methodName, annotationName);

		// parameter
		List<AnnotationParameter> parameters = info.getParameters();
		for (AnnotationParameter parameter : parameters) {
			String propertyName = new StringBuilder(prefix).append('/').append(parameter.getName()).toString();
			String parameterType = parameter.getType();
			String description = parameter.getDescription();
			String defaultValue = getParameterDefaultValue(parameter, mpftAnnotation);
			String extensionName = null;
			if (annotatedClassOrMethod == null) {
				sourceType = parameter.getSourceType();
				sourceMethod = parameter.getSourceMethod();
			}
			// Enumerations
			PsiClass jdtType = parameter.getJDTType();
			super.updateHint(collector, jdtType);

			super.addItemMetadata(collector, propertyName, parameterType, description, sourceType, null, sourceMethod,
					defaultValue, extensionName, binary);
		}
	}

	private static boolean isProcessed(String className, String annotationName,
			MicroProfileFaultToleranceContext mpftContext) {
		if (mpftContext.isProcessed(className, annotationName)) {
			return true;
		}
		mpftContext.setProcessed(className, annotationName);
		return false;
	}

	private static String getParameterDefaultValue(AnnotationParameter parameter, PsiAnnotation mpftAnnotation) {
		String defaultValue = mpftAnnotation != null ? getAnnotationMemberValue(mpftAnnotation, parameter.getName())
				: null;
		return defaultValue != null ? defaultValue : parameter.getDefaultValue();
	}

	private static String createPrefix(String className, String methodName, String annotationName) {
		if (className == null && methodName == null) {
			return annotationName;
		}
		StringBuilder prefix = new StringBuilder();
		// classname
		if (className != null) {
			prefix.append(className).append('/');
		}
		// methodname
		if (methodName != null) {
			prefix.append(methodName).append('/');
		}
		// annotation
		prefix.append(annotationName);
		return prefix.toString();
	}

	@Override
	protected void processAnnotation(PsiModifierListOwner javaElement, PsiAnnotation mpftAnnotation, String annotationName,
									 SearchContext context) {
		if (!(javaElement instanceof PsiMember)) {
			return;
		}
		// The java element is method or a class
		MicroProfileFaultToleranceContext mpftContext = getMicroProfileFaultToleranceContext(context);
		AnnotationInfo info = mpftContext.getAnnotationInfo(annotationName);
		if (info != null) {
			// 1. Collect properties for <annotation>/<list of parameters>
			collectProperties(context.getCollector(), info, null, null, mpftContext);
			mpftContext.addFaultToleranceProperties(context.getCollector());
			// 2. Collect properties for <classname>/<annotation>/<list of parameters>
			if (javaElement instanceof PsiMethod) {
				PsiMethod annotatedMethod = (PsiMethod) javaElement;
				PsiClass classType = annotatedMethod.getContainingClass();
				PsiAnnotation mpftAnnotationForClass = getAnnotation(classType, annotationName);
				collectProperties(context.getCollector(), info, classType, mpftAnnotationForClass, mpftContext);
			}
			// 3. Collect properties for <classname>/<annotation>/<list of parameters> or
			// <classname>/<methodname>/<annotation>/<list of parameters>
			collectProperties(context.getCollector(), info, (PsiMember) javaElement, mpftAnnotation, mpftContext);
		}
	}

	private static MicroProfileFaultToleranceContext getMicroProfileFaultToleranceContext(SearchContext context) {
		MicroProfileFaultToleranceContext mpftContext = (MicroProfileFaultToleranceContext) context
				.get(MICROPROFILE_FAULT_TOLERANCE_CONTEXT_KEY);
		if (mpftContext == null) {
			mpftContext = new MicroProfileFaultToleranceContext(context.getModule(), context.getUtils(),
					context.getDocumentFormat());
			context.put(MICROPROFILE_FAULT_TOLERANCE_CONTEXT_KEY, mpftContext);
		}
		return mpftContext;
	}
}
