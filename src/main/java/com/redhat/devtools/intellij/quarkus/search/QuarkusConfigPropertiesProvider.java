/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.deployment.bean.JavaBeanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.join;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Properties provider to collect Quarkus properties from the Java classes or
 * interfaces annotated with "io.quarkus.arc.config.ConfigProperties"
 * annotation.
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.quarkus/src/main/java/com/redhat/microprofile/jdt/internal/quarkus/providers/QuarkusConfigPropertiesProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.quarkus/src/main/java/com/redhat/microprofile/jdt/internal/quarkus/providers/QuarkusConfigPropertiesProvider.java</a>
 *
 */
public class QuarkusConfigPropertiesProvider extends AbstractAnnotationTypeReferencePropertiesProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusConfigPropertiesProvider.class);

	private static final String[] ANNOTATION_NAMES = { QuarkusConstants.CONFIG_PROPERTIES_ANNOTATION };

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(PsiMember psiElement, PsiAnnotation annotation, String annotationName,
									 SearchContext context) {
		processConfigProperties(psiElement, annotation, context.getCollector());
	}

	// ------------- Process Quarkus ConfigProperties -------------

	private void processConfigProperties(PsiModifierListOwner psiElement, PsiAnnotation configPropertiesAnnotation,
			IPropertiesCollector collector) {
		if (!(psiElement instanceof PsiClass)) {
			return;
		}
		PsiClass configPropertiesType = (PsiClass) psiElement;
		// Location (JAR, src)
		VirtualFile packageRoot = PsiTypeUtils.getRootDirectory(psiElement);
		String location = PsiTypeUtils.getLocation(psiElement.getProject(), packageRoot);
		// Quarkus Extension name
		String extensionName = PsiQuarkusUtils.getExtensionName(location);

		String prefix = determinePrefix(configPropertiesType, configPropertiesAnnotation);
		if (configPropertiesType.isInterface()) {
			// See
			// https://github.com/quarkusio/quarkus/blob/0796d712d9a3cf8251d9d8808b705f1a04032ee2/extensions/arc/deployment/src/main/java/io/quarkus/arc/deployment/configproperties/InterfaceConfigPropertiesUtil.java#L89
			List<PsiClass> allInterfaces = new ArrayList(Arrays.asList(findInterfaces(configPropertiesType)));
			allInterfaces.add(0, configPropertiesType);

			for (PsiClass configPropertiesInterface : allInterfaces) {
				// Loop for each methods.
				PsiElement[] elements = configPropertiesInterface.getChildren();
				// Loop for each fields.
				for (PsiElement child : elements) {
					if (child instanceof PsiMethod) {
						PsiMethod method = (PsiMethod) child;
						if (method.getModifierList().hasExplicitModifier(PsiModifier.DEFAULT)) { // don't do anything with default methods
							continue;
						}
						if (method.hasParameters()) {
							LOGGER.info("Method " + method.getName() + " of interface "
											+ method.getContainingClass().getQualifiedName()
											+ " is not a getter method since it defined parameters");
							continue;
						}
						if (PsiType.VOID.equals(method.getReturnType())) {
							LOGGER.info("Method " + method.getName() + " of interface "
											+ method.getContainingClass().getQualifiedName()
											+ " is not a getter method since it returns void");
							continue;
						}
						String name = null;
						String defaultValue = null;
						PsiAnnotation configPropertyAnnotation = AnnotationUtils.getAnnotation(method,
								QuarkusConstants.CONFIG_PROPERTY_ANNOTATION);
						if (configPropertyAnnotation != null) {
							name = AnnotationUtils.getAnnotationMemberValue(configPropertyAnnotation,
									QuarkusConstants.CONFIG_PROPERTY_ANNOTATION_NAME);
							defaultValue = AnnotationUtils.getAnnotationMemberValue(configPropertyAnnotation,
									QuarkusConstants.CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE);
						}
						if (name == null) {
							name = getPropertyNameFromMethodName(method);
						}
						if (name == null) {
							continue;
						}

						String propertyName = prefix + "." + name;
						String methodResultTypeName = PsiTypeUtils.getResolvedResultTypeName(method);
						PsiClass returnType = PsiTypeUtils.findType(method.getManager(), methodResultTypeName);

						// Method result type
						String type = PsiTypeUtils.getPropertyType(returnType, methodResultTypeName);

						// TODO: extract Javadoc from Java sources
						String description = null;

						// Method source
						String sourceType = PsiTypeUtils.getSourceType(method);
						String sourceMethod = PsiTypeUtils.getSourceMethod(method);

						// Enumerations
						super.updateHint(collector, returnType);

						if (PsiTypeUtils.isSimpleFieldType(returnType, methodResultTypeName)) {
							super.addItemMetadata(collector, propertyName, type, description, sourceType, null,
									sourceMethod, defaultValue, extensionName, PsiTypeUtils.isBinary(method));
						} else {
							populateConfigObject(returnType, propertyName, extensionName, new HashSet(), collector);
						}

					}
				}
			}
		} else {
			// See
			// https://github.com/quarkusio/quarkus/blob/e8606513e1bd14f0b1aaab7f9969899bd27c55a3/extensions/arc/deployment/src/main/java/io/quarkus/arc/deployment/configproperties/ClassConfigPropertiesUtil.java#L117
			// TODO : validation
			populateConfigObject(configPropertiesType, prefix, extensionName, new HashSet<>(), collector);
		}
	}

	private static PsiClass[] findInterfaces(PsiClass type) {
		return type.getInterfaces();
	}

	private void populateConfigObject(PsiClass configPropertiesType, String prefixStr, String extensionName,
									  Set<PsiClass> typesAlreadyProcessed, IPropertiesCollector collector) {
		if (typesAlreadyProcessed.contains(configPropertiesType)) {
			return;
		}
		typesAlreadyProcessed.add(configPropertiesType);
		PsiElement[] elements = configPropertiesType.getChildren();
		// Loop for each fields.
		for (PsiElement child : elements) {
			if (child instanceof PsiField) {
				// The following code is an adaptation for JDT of
				// Quarkus arc code:
				// https://github.com/quarkusio/quarkus/blob/e8606513e1bd14f0b1aaab7f9969899bd27c55a3/extensions/arc/deployment/src/main/java/io/quarkus/arc/deployment/configproperties/ClassConfigPropertiesUtil.java#L211
				PsiField field = (PsiField) child;
				boolean useFieldAccess = false;
				String setterName = JavaBeanUtil.getSetterName(field.getName());
				String configClassInfo = configPropertiesType.getQualifiedName();
				PsiMethod setter = findMethod(configPropertiesType, setterName, field.getType());
				if (setter == null) {
					if (!field.getModifierList().hasModifierProperty(PsiModifier.PUBLIC) || field.getModifierList().hasModifierProperty(PsiModifier.FINAL)) {
						LOGGER.info("Configuration properties class " + configClassInfo
										+ " does not have a setter for field " + field
										+ " nor is the field a public non-final field");
						continue;
					}
					useFieldAccess = true;
				}
				if (!useFieldAccess && !setter.getModifierList().hasModifierProperty(PsiModifier.PUBLIC)) {
					LOGGER.info("Setter " + setterName + " of class " + configClassInfo + " must be public");
					continue;
				}

				String name = field.getName();
				// The default value is managed with assign like : 'public String suffix = "!"';
				// Getting "!" value is possible but it requires to re-parse the Java file to
				// build a DOM CompilationUnit to extract assigned value.
				final String defaultValue = null;
				String propertyName = prefixStr + "." + name;

				String fieldTypeName = PsiTypeUtils.getResolvedTypeName(field);
				PsiClass fieldClass = PsiTypeUtils.findType(field.getManager(), fieldTypeName);
				if (PsiTypeUtils.isSimpleFieldType(fieldClass, fieldTypeName)) {

					// Class type
					String type = PsiTypeUtils.getPropertyType(fieldClass, fieldTypeName);

					// Javadoc
					String description = null;

					// field and class source
					String sourceType = PsiTypeUtils.getSourceType(field);
					String sourceField = PsiTypeUtils.getSourceField(field);

					// Enumerations
					super.updateHint(collector, fieldClass);

					super.addItemMetadata(collector, propertyName, type, description, sourceType, sourceField, null,
							defaultValue, extensionName, PsiTypeUtils.isBinary(field));
				} else {
					populateConfigObject(fieldClass, propertyName, extensionName, typesAlreadyProcessed, collector);
				}
			}
		}
	}

	private static String getPropertyNameFromMethodName(PsiMethod method) {
		try {
			return JavaBeanUtil.getPropertyNameFromGetter(method.getName());
		} catch (IllegalArgumentException e) {
			LOGGER.info("Method " + method.getName() + " of interface "
					+ method.getContainingClass().getName()
					+ " is not a getter method. Either rename the method to follow getter name conventions or annotate the method with @ConfigProperty");
			return null;
		}
	}

	private static PsiMethod findMethod(PsiClass configPropertiesType, String setterName, PsiType fieldType) {
		PsiMethod[] methods = configPropertiesType.findMethodsByName(setterName, true);
		for(PsiMethod method : methods) {
			if (method.getParameterList().getParametersCount() == 1 && method.getParameterList().getParameters()[0].getType().equals(fieldType)) {
				return method;
			}
		}
		return null;
	}

	private static String determinePrefix(PsiClass configPropertiesType, PsiAnnotation configPropertiesAnnotation) {
		String fromAnnotation = getPrefixFromAnnotation(configPropertiesAnnotation);
		if (fromAnnotation != null) {
			return fromAnnotation;
		}
		return getPrefixFromClassName(configPropertiesType);
	}

	private static String getPrefixFromAnnotation(PsiAnnotation configPropertiesAnnotation) {
		String value = AnnotationUtils.getAnnotationMemberValue(configPropertiesAnnotation, "prefix");
		if (value == null) {
			return null;
		}
		if (ConfigProperties.UNSET_PREFIX.equals(value) || value.isEmpty()) {
			return null;
		}
		return value;
	}

	private static String getPrefixFromClassName(PsiClass className) {
		String simpleName = className.getName(); // className.isInner() ? className.local() :
														// className.withoutPackagePrefix();
		return join("-", withoutSuffix(lowerCase(camelHumpsIterator(simpleName)), "config", "configuration",
				"properties", "props"));
	}
}
