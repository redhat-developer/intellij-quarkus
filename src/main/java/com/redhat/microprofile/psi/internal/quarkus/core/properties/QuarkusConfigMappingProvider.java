/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.core.properties;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractAnnotationTypeReferencePropertiesProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.IPropertiesCollector;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;
import com.redhat.microprofile.psi.quarkus.PsiQuarkusUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValue;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.hasAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.findType;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getEnclosedType;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getFirstTypeParameter;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getPropertyType;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getRawResolvedTypeName;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getResolvedResultTypeName;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getResolvedTypeName;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getSourceMethod;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getSourceType;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.isPrimitiveType;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.CONFIG_MAPPING_ANNOTATION;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.CONFIG_MAPPING_ANNOTATION_NAMING_STRATEGY;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.CONFIG_MAPPING_ANNOTATION_PREFIX;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.CONFIG_MAPPING_NAMING_STRATEGY_SNAKE_CASE;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.CONFIG_MAPPING_NAMING_STRATEGY_VERBATIM;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.WITH_DEFAULT_ANNOTATION;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.WITH_DEFAULT_ANNOTATION_VALUE;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.WITH_NAME_ANNOTATION;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.WITH_NAME_ANNOTATION_VALUE;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.WITH_PARENT_NAME_ANNOTATION;
import static com.siyeh.ig.psiutils.TypeUtils.isOptional;
import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.hyphenate;
import static io.quarkus.runtime.util.StringUtil.join;
import static io.quarkus.runtime.util.StringUtil.lowerCase;

/**
 * Properties provider to collect Quarkus properties from the Java classes or
 * interfaces annotated with "io.smallrye.config.ConfigMapping" annotation.
 * 
 * @author Angelo ZERR
 * 
 * @see <a href="https://quarkus.io/guides/config-mappings">https://quarkus.io/guides/config-mappings</a>
 */
public class QuarkusConfigMappingProvider extends AbstractAnnotationTypeReferencePropertiesProvider {

	private static final Logger LOGGER = Logger.getLogger(QuarkusConfigMappingProvider.class.getName());

	private static final String[] ANNOTATION_NAMES = { CONFIG_MAPPING_ANNOTATION };

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(PsiModifierListOwner javaElement, PsiAnnotation annotation, String annotationName,
									 SearchContext context) {
		processConfigMapping(javaElement, annotation, context.getCollector());
	}

	// ------------- Process Quarkus ConfigMapping -------------

	private void processConfigMapping(PsiModifierListOwner javaElement, PsiAnnotation configMappingAnnotation,
									  IPropertiesCollector collector) {
		if (!(javaElement instanceof PsiClass)) {
			return;
		}
		PsiClass configMappingType = (PsiClass) javaElement;
		if (!configMappingType.isInterface()) {
			// @ConfigMapping can be used only with interfaces.
			return;
		}
		// Location (JAR, src)
		VirtualFile packageRoot = PsiTypeUtils.getRootDirectory(PsiTreeUtil.getParentOfType(javaElement, PsiFile.class));
		String location = PsiTypeUtils.getLocation(javaElement.getProject(), packageRoot);
		// Quarkus Extension name
		String extensionName = PsiQuarkusUtils.getExtensionName(location);

		String prefix = getPrefixFromAnnotation(configMappingAnnotation);
		if (prefix == null || prefix.trim().isEmpty()) {
			// @ConfigMapping has no prefix
			return;
		}
		// @ConfigMapping(prefix="server") case
		List<PsiClass> allInterfaces = new ArrayList<>(Arrays.asList(findInterfaces(configMappingType)));
		allInterfaces.add(0, configMappingType);
		for (PsiClass configMappingInterface : allInterfaces) {
			populateConfigObject(configMappingInterface, prefix, extensionName, new HashSet<>(),
					configMappingAnnotation, collector);
		}
	}

	private static PsiClass[] findInterfaces(PsiClass type) {
		return type.getInterfaces();
	}

	private void populateConfigObject(PsiClass configMappingType, String prefixStr, String extensionName,
			Set<PsiClass> typesAlreadyProcessed, PsiAnnotation configMappingAnnotation, IPropertiesCollector collector) {
		if (typesAlreadyProcessed.contains(configMappingType)) {
			return;
		}
		typesAlreadyProcessed.add(configMappingType);
		PsiElement[] elements = configMappingType.getChildren();
		// Loop for each methods
		for (PsiElement child : elements) {
			if (child instanceof PsiMethod) {
				PsiMethod method = (PsiMethod) child;
				if (method.getReturnType() == null || method.getModifierList().hasExplicitModifier(PsiModifier.DEFAULT) || method.hasParameters()
						|| PsiType.VOID.equals(method.getReturnType())) {
					continue;
				}

				PsiType psiType = method.getReturnType();
				String returnTypeSignature = getResolvedResultTypeName(method);
				String resolvedTypeSignature = getRawResolvedTypeName(method);
				if (isOptional(psiType)) {
					// it's an optional type
					// Optional<List<String>> databases();
					// extract the type List<String>
					psiType = getFirstTypeParameter(psiType);
					if (psiType != null) {
						resolvedTypeSignature = getRawResolvedTypeName(psiType);
					}
				}

				PsiClass returnType = findType(method.getManager(), resolvedTypeSignature);
				boolean simpleType = isSimpleType(resolvedTypeSignature, returnType);

				if (!simpleType) {
					if (returnType != null
							&& !returnType.isInterface()) {
						// When type is not an interface, it requires Converters
						// ex :
						// interface Server {Log log; class Log {}}
						// throw the error;
						// java.lang.IllegalArgumentException: SRCFG00013: No Converter registered for
						// class org.acme.Server2$Log
						// at
						// io.smallrye.config.SmallRyeConfig.requireConverter(SmallRyeConfig.java:466)
						// at
						// io.smallrye.config.ConfigMappingContext.getConverter(ConfigMappingContext.java:113)
						continue;
					}
				}

				String defaultValue = getWithDefault(method);
				String propertyName = getPropertyName(method, prefixStr, configMappingAnnotation);
				// Method result type
				String type = getPropertyType(returnType, resolvedTypeSignature);

				// TODO: extract Javadoc from Java sources
				String description = null;

				// Method source
				String sourceType = getSourceType(method);
				String sourceMethod = getSourceMethod(method);

				// Enumerations
				PsiClass enclosedType = getEnclosedType(returnType, resolvedTypeSignature, method.getManager());
				super.updateHint(collector, enclosedType);

				if (!simpleType) {
					if (isMap(returnType, resolvedTypeSignature)) {
						// Map<String, String>
						propertyName += ".{*}";
						simpleType = true;
					} else if (isCollection(returnType, resolvedTypeSignature)) {
						// List<String>, List<App>
						propertyName += "[*]"; // Generate indexed property.
						String genericTypeName = getResolvedTypeName(((PsiClassType) psiType).getParameters()[0]);
						resolvedTypeSignature = getRawResolvedTypeName(((PsiClassType) psiType).getParameters()[0]);
						returnType = findType(method.getManager(), resolvedTypeSignature);
						simpleType = isSimpleType(resolvedTypeSignature, returnType);
					}
				}

				if (simpleType) {
					// String, int, Optional, etc
					ItemMetadata metadata = super.addItemMetadata(collector, propertyName, type, description,
							sourceType, null, sourceMethod, defaultValue, extensionName, PsiTypeUtils.isBinary(method));
					PsiQuarkusUtils.updateConverterKinds(metadata, method, enclosedType);
				} else {
					// Other type (App, etc)
					populateConfigObject(returnType, propertyName, extensionName, typesAlreadyProcessed,
							configMappingAnnotation, collector);
				}
			}
		}
	}

	private boolean isSimpleType(String resolvedTypeSignature, PsiClass returnType) {
		return returnType == null
				|| isPrimitiveType(resolvedTypeSignature)
				|| isSimpleOptionalType(resolvedTypeSignature)
				|| returnType.isEnum();
	}

	private boolean isSimpleOptionalType(String resolvedTypeSignature) {
		return "java.util.OptionalInt".equals(resolvedTypeSignature)
				|| "java.util.OptionalDouble".equals(resolvedTypeSignature)
				|| "java.util.OptionalLong".equals(resolvedTypeSignature);
	}

	private static boolean isMap(PsiClass type, String typeName) {
		// Fast check
		if (typeName.startsWith("java.util.Map") || typeName.startsWith("java.util.SortedMap")) {
			return true;
		}
		// TODO : check if type extends Map
		return false;
	}

	private static boolean isCollection(PsiClass type, String typeName) {
		// Fast check
		if (typeName.startsWith("java.util.Collection") || typeName.startsWith("java.util.Set")
				|| typeName.startsWith("java.util.SortedSet") || typeName.startsWith("java.util.List")) {
			return true;
		}
		// TODO : check if type extends Collection
		return false;
	}

	private String getPropertyName(PsiMember member, String prefix, PsiAnnotation configMappingAnnotation) {
		if (hasAnnotation(member, WITH_PARENT_NAME_ANNOTATION)) {
			return prefix;
		}
		return prefix + "." + convertName(member, configMappingAnnotation);
	}

	private static String convertName(PsiMember member, PsiAnnotation configMappingAnnotation) {
		// 1) Check if @WithName is used
		// @WithName("name")
		// String host();
		// --> See https://quarkus.io/guides/config-mappings#withname
		PsiAnnotation withNameAnnotation = getAnnotation(member, WITH_NAME_ANNOTATION);
		if (withNameAnnotation != null) {
			String name = getAnnotationMemberValue(withNameAnnotation, WITH_NAME_ANNOTATION_VALUE);
			if (StringUtils.isNotEmpty(name)) {
				return name;
			}
		}

		String name = member.getName();

		// 2) Check if ConfigMapping.NamingStrategy is used
		// @ConfigMapping(prefix = "server", namingStrategy =
		// ConfigMapping.NamingStrategy.VERBATIM)
		// public interface ServerVerbatimNamingStrategy
		// --> See https://quarkus.io/guides/config-mappings#namingstrategy
		String namingStrategy = getAnnotationMemberValue(configMappingAnnotation,
				CONFIG_MAPPING_ANNOTATION_NAMING_STRATEGY);
		if (namingStrategy != null) {
			int index = namingStrategy.lastIndexOf('.');
			if (index != -1) {
				namingStrategy = namingStrategy.substring(index + 1, namingStrategy.length());
			}
			switch (namingStrategy) {
			case CONFIG_MAPPING_NAMING_STRATEGY_VERBATIM:
				// The method name is used as is to map the configuration property.
				return name;
			case CONFIG_MAPPING_NAMING_STRATEGY_SNAKE_CASE:
				// The method name is derived by replacing case changes with an underscore to
				// map the configuration property.
				return snake(name);
			default:
				// KEBAB_CASE
				// The method name is derived by replacing case changes with a dash to map the
				// configuration property.
				return hyphenate(name);
			}
		}

		// None namingStrategy, use KEBAB_CASE as default
		return hyphenate(name);
	}

	/**
	 * Returns the value of @WithDefault("a value") and null otherwise.
	 * 
	 * @param member the filed, method which is annotated with @WithDefault. s
	 * @return the value of @WithDefault("a value") and null otherwise.
	 */
	private static String getWithDefault(PsiMember member) {
		PsiAnnotation withDefaultAnnotation = getAnnotation(member, WITH_DEFAULT_ANNOTATION);
		if (withDefaultAnnotation != null) {
			String defaultValue = getAnnotationMemberValue(withDefaultAnnotation, WITH_DEFAULT_ANNOTATION_VALUE);
			if (StringUtils.isNotEmpty(defaultValue)) {
				return defaultValue;
			}
		}
		return null;
	}

	private static String getPrefixFromAnnotation(PsiAnnotation configMappingAnnotation) {
		String value = getAnnotationMemberValue(configMappingAnnotation, CONFIG_MAPPING_ANNOTATION_PREFIX);
		if (value == null || value.isEmpty()) {
			return null;
		}
		return value;
	}

	private static String snake(String orig) {
		return join("_", lowerCase(camelHumpsIterator(orig)));
	}
}
