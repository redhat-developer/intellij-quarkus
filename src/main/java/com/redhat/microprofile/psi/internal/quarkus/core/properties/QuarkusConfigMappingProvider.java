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
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractAnnotationTypeReferencePropertiesProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.IPropertiesCollector;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;
import com.redhat.microprofile.psi.quarkus.PsiQuarkusUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValue;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.hasAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.findType;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getEnclosedType;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getFirstTypeParameter;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getPropertyType;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getRawResolvedTypeName;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getResolvedTypeName;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getSourceMethod;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getSourceType;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.CONFIG_MAPPING_ANNOTATION;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.CONFIG_MAPPING_ANNOTATION_NAMING_STRATEGY;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.CONFIG_MAPPING_ANNOTATION_PREFIX;
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
 * @see <a href="https://quarkus.io/guides/config-mappings">https://quarkus.io/guides/config-mappings</a>
 */
public class QuarkusConfigMappingProvider extends AbstractAnnotationTypeReferencePropertiesProvider {

    private static final String[] ANNOTATION_NAMES = {CONFIG_MAPPING_ANNOTATION};

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
        if (!(javaElement instanceof PsiClass configMappingType)) {
            return;
        }
        if (!configMappingType.isInterface()) {
            // @ConfigMapping can be used only with interfaces.
            return;
        }
        // Location (JAR, src)
        VirtualFile packageRoot = PsiTypeUtils.getRootDirectory(PsiTreeUtil.getParentOfType(javaElement, PsiFile.class));
        String location = packageRoot != null ? packageRoot.getUrl() : null;
        // Quarkus Extension name
        String extensionName = PsiQuarkusUtils.getExtensionName(location);

        String prefix = getPrefixFromAnnotation(configMappingAnnotation);
        if (prefix == null || prefix.trim().isEmpty()) {
            // @ConfigMapping has no prefix
            return;
        }
        // @ConfigMapping(prefix="server") case
        Set<PsiClass> allInterfaces = findInterfaces(configMappingType);
        for (PsiClass configMappingInterface : allInterfaces) {
            populateConfigObject(configMappingInterface, prefix, extensionName, new HashSet<>(),
                    configMappingAnnotation, collector);
        }
    }

    private static Set<PsiClass> findInterfaces(@NotNull PsiClass type) {
        // No reason to use a JDK interface to generate a config class? Primarily to fix the java.nio.file.Path case.
        // see https://github.com/smallrye/smallrye-config/blob/22635f24dc7634706867cc52e28d5bd82d15f54e/implementation/src/main/java/io/smallrye/config/ConfigMappingInterface.java#L782C9-L783C58
        if (type.getQualifiedName() == null || type.getQualifiedName().startsWith("java")) {
            return Collections.emptySet();
        }
        Set<PsiClass> result = new HashSet<>();
        result.add(type);
        collectInterfaces(type, result);
        return result;
    }

    private static void collectInterfaces(@Nullable PsiClass cls, @NotNull Set<PsiClass> result) {
        if (cls == null) return;

        // Direct interfaces
        for (PsiClass iface : cls.getInterfaces()) {
            if (result.add(iface)) {
                collectInterfaces(iface, result);
            }
        }

        // Parent interfaces
        PsiClass superClass = cls.getSuperClass();
        if (superClass != null) {
            collectInterfaces(superClass, result);
        }
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
            if (child instanceof PsiMethod method) {
                if (method.getReturnType() == null || method.getModifierList().hasExplicitModifier(PsiModifier.DEFAULT) || method.hasParameters()
                        || PsiTypeUtils.isVoidReturnType(method)) {
                    continue;
                }

                PsiType psiType = method.getReturnType();
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
                boolean leafType = isLeafType( returnType);

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

                boolean iterable = false;
                if (!leafType) {
                    if (isMap(returnType, resolvedTypeSignature)) {
                        iterable = true;
                        // Map<String, String>
                        // Map<String, SomeConfig>
                        propertyName += ".{*}";
                        var parameters = ((PsiClassType) psiType).getParameters();
                        if (parameters.length > 1) {
                            returnType = (parameters[1] instanceof PsiClassReferenceType parameterClassType) ? parameterClassType.resolve() : null;
                            leafType = isLeafType(returnType);
                        } else {
                            leafType = false;
                        }
                    } else if (isCollection(returnType, resolvedTypeSignature)) {
                        iterable = true;
                        // List<String>, List<App>
                        propertyName += "[*]"; // Generate indexed property.
                        var parameters = ((PsiClassType) psiType).getParameters();
                        if (parameters.length > 0) {
                            returnType = (parameters[0] instanceof PsiClassReferenceType parameterClassType) ? parameterClassType.resolve() : null;
                            leafType = isLeafType(returnType);
                        } else {
                            leafType = false;
                        }
                    }
                }

                if (leafType) {
                    // String, int, Optional, or Class (not interface)
                    ItemMetadata metadata = super.addItemMetadata(collector, propertyName, type, description,
                            sourceType, null, sourceMethod, defaultValue, extensionName, PsiTypeUtils.isBinary(method));
                    PsiQuarkusUtils.updateConverterKinds(metadata, method, enclosedType);
                } else {
                    // Other type (App interface, etc)
                    Set<PsiClass> allInterfaces = findInterfaces(returnType);
                    for (PsiClass configMappingInterface : allInterfaces) {
                        populateConfigObject(configMappingInterface, propertyName, extensionName, typesAlreadyProcessed,
                                configMappingAnnotation, collector);
                    }
                }
            }
        }
    }

    /**
     * Returns true if the given return type should be treated as a leaf in the configuration tree,
     * i.e. it is null or not an interface, and therefore not recursively visited.
     */
    private static boolean isLeafType(@Nullable PsiClass returnType) {
        return returnType == null || !returnType.isInterface();
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
        NamingStrategy namingStrategy = getNamingStrategy(configMappingAnnotation);
        if (namingStrategy != null) {
            switch (namingStrategy) {
                case VERBATIM:
                    // The method name is used as is to map the configuration property.
                    return name;
                case SNAKE_CASE:
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
     * Returns the Quarkus @ConfigRoot(phase=...) value.
     *
     * @param configMappingAnnotation
     * @return the Quarkus @ConfigRoot(phase=...) value.
     */
    private static NamingStrategy getNamingStrategy(PsiAnnotation configMappingAnnotation) {
        // 2) Check if ConfigMapping.NamingStrategy is used
        // @ConfigMapping(prefix = "server", namingStrategy =
        // ConfigMapping.NamingStrategy.VERBATIM)
        // public interface ServerVerbatimNamingStrategy
        // --> See https://quarkus.io/guides/config-mappings#namingstrategy
        String namingStrategy = getAnnotationMemberValue(configMappingAnnotation,
                CONFIG_MAPPING_ANNOTATION_NAMING_STRATEGY);
        if (namingStrategy != null) {
            try {
                return NamingStrategy.valueOf(namingStrategy.toUpperCase());
            }
            catch(Exception e) {

            }
        }
        return null;
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
