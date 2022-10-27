/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.core.properties;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractAnnotationTypeReferencePropertiesProvider;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.IPropertiesCollector;
import com.redhat.microprofile.psi.internal.quarkus.providers.QuarkusContext;
import com.redhat.microprofile.psi.internal.quarkus.providers.QuarkusSearchContext;
import com.redhat.microprofile.psi.quarkus.PsiQuarkusUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getEnclosedType;
import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.hyphenate;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.lowerCaseFirst;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;

/**
 * Properties provider to collect Quarkus properties from the Java classes
 * annotated with "io.quarkus.runtime.annotations.ConfigRoot" annotation.
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.quarkus/src/main/java/com/redhat/microprofile/jdt/internal/quarkus/providers/QuarkusConfigRootProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.quarkus/src/main/java/com/redhat/microprofile/jdt/internal/quarkus/providers/QuarkusConfigRootProvider.java</a>
 *
 */
public class QuarkusConfigRootProvider extends AbstractAnnotationTypeReferencePropertiesProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusConfigRootProvider.class);

	private static final String[] ANNOTATION_NAMES = { QuarkusConstants.CONFIG_ROOT_ANNOTATION };

	private static final String JAVADOC_CACHE_KEY = QuarkusConfigRootProvider.class.getName() + "#javadoc";

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	public void beginSearch(SearchContext context) {
		Map<VirtualFile, Properties> javadocCache = new HashMap();
		context.put(JAVADOC_CACHE_KEY, javadocCache);
	}


	@Override
	protected void processAnnotation(PsiModifierListOwner psiElement, PsiAnnotation annotation, String annotationName,
									 SearchContext context) {
		Map<VirtualFile, Properties> javadocCache = (Map<VirtualFile, Properties>) context
				.get(JAVADOC_CACHE_KEY);
		QuarkusSearchContext quarkusContext = QuarkusSearchContext.getQuarkusContext(context);
		processConfigRoot(psiElement, annotation, javadocCache, quarkusContext, context.getCollector());
	}

	// ------------- Process Quarkus ConfigRoot -------------

	/**
	 * Process Quarkus ConfigRoot annotation from the given
	 * <code>psiElement</code>.
	 * 
	 * @param psiElement          the class, field element which have a Quarkus
	 *                             ConfigRoot annotations
	 * @param configRootAnnotation the Quarkus ConfigRoot annotation
	 * @param javadocCache         the documentation cache
	 * @param collector            the properties to fill
	 */
	private void processConfigRoot(PsiModifierListOwner psiElement, PsiAnnotation configRootAnnotation,
			Map<VirtualFile, Properties> javadocCache, QuarkusSearchContext quarkusContext, IPropertiesCollector collector) {
		ConfigPhase configPhase = getConfigPhase(configRootAnnotation);
		String configRootAnnotationName = getConfigRootName(configRootAnnotation);
		String extension = getExtensionName(getSimpleName(psiElement), configRootAnnotationName, configPhase);
		if (extension == null) {
			return;
		}
		// Location (JAR, src)
		VirtualFile packageRoot = PsiTypeUtils.getRootDirectory(PsiTreeUtil.getParentOfType(psiElement, PsiFile.class));
		String location = PsiTypeUtils.getLocation(psiElement.getProject(), packageRoot);
		// Quarkus Extension name
		String extensionName = PsiQuarkusUtils.getExtensionName(location);

		String baseKey = extension.isEmpty() ? QuarkusConstants.QUARKUS_PREFIX
				: QuarkusConstants.QUARKUS_PREFIX + '.' + extension;
		processConfigGroup(extensionName, psiElement, baseKey, configPhase, javadocCache, quarkusContext, collector);
	}

	/**
	 * Returns the Quarkus @ConfigRoot(phase=...) value.
	 * 
	 * @param configRootAnnotation
	 * @return the Quarkus @ConfigRoot(phase=...) value.
	 */
	private static ConfigPhase getConfigPhase(PsiAnnotation configRootAnnotation) {
		String value = AnnotationUtils.getAnnotationMemberValue(configRootAnnotation, QuarkusConstants.CONFIG_ROOT_ANNOTATION_PHASE);
		if (value != null) {
			if (value.endsWith(ConfigPhase.RUN_TIME.name())) {
				return ConfigPhase.RUN_TIME;
			}
			if (value.endsWith(ConfigPhase.BUILD_AND_RUN_TIME_FIXED.name())) {
				return ConfigPhase.BUILD_AND_RUN_TIME_FIXED;
			}
		}
		return ConfigPhase.BUILD_TIME;
	}

	/**
	 * Returns the Quarkus @ConfigRoot(name=...) value and
	 * {@link ConfigItem#HYPHENATED_ELEMENT_NAME} otherwise.
	 * 
	 * @param configRootAnnotation @ConfigRoot annotation
	 * @return the Quarkus @ConfigRoot(name=...) value and
	 *         {@link ConfigItem#HYPHENATED_ELEMENT_NAME} otherwise.
	 */
	private static String getConfigRootName(PsiAnnotation configRootAnnotation) {
		String value = AnnotationUtils.getAnnotationMemberValue(configRootAnnotation, QuarkusConstants.CONFIG_ANNOTATION_NAME);
		if (value != null) {
			return value;
		}
		return ConfigItem.HYPHENATED_ELEMENT_NAME;
	}

	/**
	 * Returns the simple name of the given <code>javaElement</code>
	 * 
	 * @param javaElement the Java class element
	 * @return the simple name of the given <code>javaElement</code>
	 */
	private static String getSimpleName(PsiModifierListOwner javaElement) {
		String elementName = javaElement instanceof NavigationItem?((NavigationItem)javaElement).getName():"";
		int index = elementName.lastIndexOf('.');
		return index != -1 ? elementName.substring(index + 1, elementName.length()) : elementName;
	}

	/**
	 * Returns the Quarkus extension name according the
	 * <code>configRootClassSimpleName</code>, <code>configRootAnnotationName</code>
	 * and <code>configPhase</code>.
	 * 
	 * @param configRootClassSimpleName the simple class name where ConfigRoot is
	 *                                  declared.
	 * @param configRootAnnotationName  the name declared in the ConfigRoot
	 *                                  annotation.
	 * @param configPhase               the config phase.
	 * @see <a href="https://github.com/quarkusio/quarkus/blob/master/core/deployment/src/main/java/io/quarkus/deployment/configuration/ConfigDefinition.java#L173">
	 *      (registerConfigRoot)</a>
	 * @return the Quarkus extension name according the
	 *         <code>configRootClassSimpleName</code>,
	 *         <code>configRootAnnotationName</code> and <code>configPhase</code>.
	 */
	private static String getExtensionName(String configRootClassSimpleName, String configRootAnnotationName,
			ConfigPhase configPhase) {
		// See
		// https://github.com/quarkusio/quarkus/blob/master/core/deployment/src/main/java/io/quarkus/deployment/configuration/ConfigDefinition.java#L173
		// registerConfigRoot
		String rootName = configRootAnnotationName;
		final List<String> segments = toList(camelHumpsIterator(configRootClassSimpleName));
		final List<String> trimmedSegments;
		if (configPhase == ConfigPhase.RUN_TIME) {
			trimmedSegments = withoutSuffix(
					withoutSuffix(
							withoutSuffix(
									withoutSuffix(withoutSuffix(withoutSuffix(segments, "Runtime", "Configuration"),
											"Runtime", "Config"), "Run", "Time", "Configuration"),
									"Run", "Time", "Config"),
							"Configuration"),
					"Config");
		} else if (configPhase == ConfigPhase.BOOTSTRAP) {
			trimmedSegments = withoutSuffix(withoutSuffix(
					withoutSuffix(withoutSuffix(segments, "Bootstrap", "Configuration"), "Bootstrap", "Config"),
					"Configuration"), "Config");
		} else {
			trimmedSegments = withoutSuffix(withoutSuffix(
					withoutSuffix(withoutSuffix(segments, "Build", "Time", "Configuration"), "Build", "Time", "Config"),
					"Configuration"), "Config");
		}
		if (rootName.equals(ConfigItem.PARENT)) {
			rootName = "";
		} else if (rootName.equals(ConfigItem.ELEMENT_NAME)) {
			rootName = String.join("", (Iterable<String>) () -> lowerCaseFirst(trimmedSegments.iterator()));
		} else if (rootName.equals(ConfigItem.HYPHENATED_ELEMENT_NAME)) {
			rootName = String.join("-", (Iterable<String>) () -> lowerCase(trimmedSegments.iterator()));
		}
		return rootName;
	}

	private static <T> List<T> toList(Iterator<T> iterator) {
		List<T> actualList = new ArrayList<>();
		iterator.forEachRemaining(actualList::add);
		return actualList;
	}

	// ------------- Process Quarkus ConfigGroup -------------

	/**
	 * Process Quarkus ConfigGroup annotation from the given
	 * <code>psiElement</code>.
	 * 
	 * @param extensionName the Quarkus extension name
	 * 
	 * @param psiElement   the class, field element which have a Quarkus
	 *                      ConfigGroup annotations.
	 * @param baseKey       the base key
	 * @param configPhase   the phase
	 * @param javadocCache  the Javadoc cache
	 * @param collector     the properties to fill.
	 */
	private void processConfigGroup(String extensionName, PsiModifierListOwner psiElement, String baseKey,
			ConfigPhase configPhase, Map<VirtualFile, Properties> javadocCache,
									QuarkusSearchContext quarkusContext, IPropertiesCollector collector) {
		if (psiElement instanceof PsiClass) {
			PsiElement[] elements = psiElement.getChildren();
			for (PsiElement child : elements) {
				if (child instanceof PsiField) {
					PsiField field = (PsiField) child;
					if (!canProcess(field)) {
						continue;
					}
					final PsiAnnotation configItemAnnotation = AnnotationUtils.getAnnotation(field,
							QuarkusConstants.CONFIG_ITEM_ANNOTATION);
					String name = configItemAnnotation == null ? hyphenate(field.getName())
							: AnnotationUtils.getAnnotationMemberValue(configItemAnnotation, QuarkusConstants.CONFIG_ANNOTATION_NAME);
					if (name == null) {
						name = ConfigItem.HYPHENATED_ELEMENT_NAME;
					}
					String subKey;
					if (name.equals(ConfigItem.PARENT)) {
						subKey = baseKey;
					} else if (name.equals(ConfigItem.ELEMENT_NAME)) {
						subKey = baseKey + "." + field.getName();
					} else if (name.equals(ConfigItem.HYPHENATED_ELEMENT_NAME)) {
						subKey = baseKey + "." + hyphenate(field.getName());
					} else {
						subKey = baseKey + "." + name;
					}
					final String defaultValue = configItemAnnotation == null ? ConfigItem.NO_DEFAULT
							: AnnotationUtils.getAnnotationMemberValue(configItemAnnotation,
									QuarkusConstants.CONFIG_ITEM_ANNOTATION_DEFAULT_VALUE);

					String fieldTypeName = PsiTypeUtils.getResolvedTypeName(field);
					PsiClass fieldClass = PsiTypeUtils.findType(field.getManager(), fieldTypeName);
					final PsiAnnotation configGroupAnnotation = AnnotationUtils.getAnnotation(fieldClass,
							QuarkusConstants.CONFIG_GROUP_ANNOTATION);
					if (configGroupAnnotation != null) {
						processConfigGroup(extensionName, fieldClass, subKey, configPhase, javadocCache, quarkusContext,
								collector);
					} else {
						addItemMetadata(extensionName, field, fieldTypeName, fieldClass, subKey, defaultValue,
								javadocCache, configPhase, quarkusContext, collector);
					}
				}
			}
		}
	}

	/**
	 * Returns true if the given field can generate a Quarkus property and false
	 * otherwise.
	 *
	 * @param field
	 * @return true if the given field can generate a Quarkus property and false
	 *         otherwise.
	 */
	private static boolean canProcess(PsiField field) {
		PsiModifierList flags = field.getModifierList();
		return !flags.hasModifierProperty(PsiModifier.STATIC) && !flags.hasModifierProperty(PsiModifier.PRIVATE);
	}


	private void addItemMetadata(String extensionName, PsiField field, String fieldTypeName, PsiClass fieldClass,
			String name, String defaultValue, Map<VirtualFile, Properties> javadocCache,
			ConfigPhase configPhase, QuarkusSearchContext quarkusContext, IPropertiesCollector collector) {

		// Class type
		String type = PsiTypeUtils.getPropertyType(fieldClass, fieldTypeName);

		// Javadoc
		String description = getJavadoc(field, javadocCache);

		// field and class source
		String sourceType = PsiTypeUtils.getSourceType(field);
		String sourceField = PsiTypeUtils.getSourceField(field);

		// Enumerations
		PsiClass enclosedType = getEnclosedType(fieldClass, type, field.getManager());
		super.updateHint(collector, enclosedType);

		ItemMetadata item = null;
		// Default value for primitive type
		if (PsiTypeUtils.isPrimitiveBoolean(fieldTypeName)) {
			item = super.addItemMetadata(collector, name, type, description, sourceType, sourceField, null,
					defaultValue == null || ConfigItem.NO_DEFAULT.equals(defaultValue) ? "false" : defaultValue,
					extensionName, PsiTypeUtils.isBinary(field));
		} else if (PsiTypeUtils.isNumber(fieldTypeName)) {
			item = super.addItemMetadata(collector, name, type, description, sourceType, sourceField, null,
					defaultValue == null || ConfigItem.NO_DEFAULT.equals(defaultValue) ? "0" : defaultValue,
					extensionName, PsiTypeUtils.isBinary(field));
		} else if (PsiTypeUtils.isMap(fieldTypeName)) {
			// FIXME: find better mean to check field is a Map
			// this code works only if user uses Map as declaration and not if they declare
			// HashMap for instance
			String[] rawTypeParameters = getRawTypeParameters(fieldTypeName);
			if ((rawTypeParameters[0].trim().equals("java.lang.String"))) {
				// The key Map must be a String
				processMap(field, name, rawTypeParameters[1], description, extensionName, sourceType, configPhase,
						javadocCache, 0, quarkusContext, collector);
			}
		} else if (PsiTypeUtils.isList(fieldTypeName)) {
			item = super.addItemMetadata(collector, name, type, description, sourceType, sourceField, null,
					defaultValue, extensionName, PsiTypeUtils.isBinary(field));
		} else if (PsiTypeUtils.isOptional(fieldTypeName)) {
			item = super.addItemMetadata(collector, name, type, description, sourceType, sourceField, null,
					defaultValue, extensionName, PsiTypeUtils.isBinary(field));
			item.setRequired(false);
		} else {
			if (ConfigItem.NO_DEFAULT.equals(defaultValue)) {
				defaultValue = null;
			}
			item = super.addItemMetadata(collector, name, type, description, sourceType, sourceField, null,
					defaultValue, extensionName, PsiTypeUtils.isBinary(field));
		}
		if (item != null) {
			item.setPhase(getPhase(configPhase));
		}
		PsiQuarkusUtils.updateConverterKinds(item, field, enclosedType);
	}

	private static String[] getRawTypeParameters(String fieldTypeName) {
		int start = fieldTypeName.indexOf("<") + 1;
		int end = fieldTypeName.lastIndexOf(">");
		String keyValue = fieldTypeName.substring(start, end);
		int index = keyValue.indexOf(',');
		return new String[] { keyValue.substring(0, index), keyValue.substring(index + 1, keyValue.length()) };
	}

	private void processMap(PsiField field, String baseKey, String mapValueClass, String docs, String extensionName,
							String source, ConfigPhase configPhase, Map<VirtualFile, Properties> javadocCache, int keyIndex,
							QuarkusSearchContext quarkusContext, IPropertiesCollector collector) {
		final String subKey = baseKey + "." + quarkusContext.getPropertyMapKey(extensionName, baseKey, keyIndex);
		if ("java.util.Map".equals(mapValueClass)) {
			// ignore, Map must be parameterized
		} else if (PsiTypeUtils.isMap(mapValueClass)) {
			String[] rawTypeParameters = getRawTypeParameters(mapValueClass);
			processMap(field, subKey, rawTypeParameters[1], docs, extensionName, source, configPhase, javadocCache,
					keyIndex + 1, quarkusContext, collector);
		} else if (PsiTypeUtils.isOptional(mapValueClass)) {
			// Optionals are not allowed as a map value type
		} else {
			PsiClass type = PsiTypeUtils.findType(field.getManager(), mapValueClass);
			if (type == null || PsiTypeUtils.isPrimitiveType(mapValueClass)) {
				// This case comes from when mapValueClass is:
				// - Simple type, like java.lang.String
				// - Type which cannot be found (bad classpath?)
				addItemMetadata(extensionName, field, mapValueClass, null, subKey, null, javadocCache, configPhase,
						quarkusContext, collector);
			} else {
				processConfigGroup(extensionName, type, subKey, configPhase, javadocCache, quarkusContext, collector);
			}
		}
	}

	/**
	 * Returns the Javadoc from the given field. There are 3 strategies to extract
	 * the Javadoc:
	 * 
	 * <ul>
	 * <li>try to extract Javadoc from the source (from '.java' source file or from
	 * JAR which is linked to source).</li>
	 * <li>try to get Javadoc from the attached Javadoc.</li>
	 * <li>get Javadoc from the Quarkus properties file stored in JAR META-INF/
	 * </ul>
	 * 
	 * @param field the field to process
	 * @param javadocCache the Javadoc cache
	 * @return the doc entry for the field
	 */
	private static String getJavadoc(PsiField field, Map<VirtualFile, Properties> javadocCache) {
		// TODO: get Javadoc from source anad attached doc by processing Javadoc tag as
		// markdown
		// Try to get javadoc from sources
		/*
		 * String javadoc = findJavadocFromSource(field); if (javadoc != null) { return
		 * javadoc; } // Try to get attached javadoc javadoc =
		 * field.getAttachedJavadoc(monitor); if (javadoc != null) { return javadoc; }
		 */
		// Try to get the javadoc inside the META-INF/quarkus-javadoc.properties of the
		// JAR
		VirtualFile packageRoot = PsiTypeUtils.getRootDirectory(PsiTreeUtil.getParentOfType(field, PsiFile.class));;
		Properties properties = javadocCache.get(packageRoot);
		if (properties == null) {
			properties = new Properties();
			javadocCache.put(packageRoot, properties);
			String quarkusJavadocResource = findJavadocFromQuakusJavadocProperties(packageRoot);
			if (quarkusJavadocResource != null) {
				try (Reader reader = new StringReader(quarkusJavadocResource)) {
					properties.load(reader);
				} catch (Exception e) {
					// TODO : log it
					e.printStackTrace();
				}
			}
		}
		if (properties.isEmpty()) {
			return null;
		}
		// The META-INF/quarkus-javadoc.properties stores Javadoc without $ . Ex:
		// io.quarkus.deployment.SslProcessor.SslConfig.native_=Enable native SSL
		// support.

		String fieldKey = field.getContainingClass().getQualifiedName() + "." + field.getName();

		// Here field key contains '$'
		// Ex : io.quarkus.deployment.SslProcessor$SslConfig.native_
		// replace '$' with '.'
		fieldKey = fieldKey.replace('$', '.');
		return properties.getProperty(fieldKey);
	}

	private static String findJavadocFromQuakusJavadocProperties(VirtualFile packageRoot) {
		return PsiTypeUtils.findPropertiesResource(packageRoot, QuarkusConstants.QUARKUS_JAVADOC_PROPERTIES_FILE);
	}

	private static int getPhase(ConfigPhase configPhase) {
		switch (configPhase) {
		case BUILD_TIME:
			return ItemMetadata.CONFIG_PHASE_BUILD_TIME;
		case BUILD_AND_RUN_TIME_FIXED:
			return ItemMetadata.CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED;
		case RUN_TIME:
			return ItemMetadata.CONFIG_PHASE_RUN_TIME;
		default:
			return ItemMetadata.CONFIG_PHASE_BUILD_TIME;
		}
	}
}