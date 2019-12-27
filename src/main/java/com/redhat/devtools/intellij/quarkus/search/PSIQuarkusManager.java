/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.EmptyQuery;
import com.intellij.util.MergeQuery;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;
import com.redhat.quarkus.commons.EnumItem;
import com.redhat.quarkus.commons.ExtendedConfigDescriptionBuildItem;
import com.redhat.quarkus.commons.QuarkusProjectInfoParams;
import com.redhat.quarkus.commons.QuarkusPropertiesScope;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CONFIG_GROUP_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CONFIG_ITEM_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CONFIG_PROPERTY_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CONFIG_ROOT_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_JAVADOC_PROPERTIES;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_PREFIX;
import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.hyphenate;
import static io.quarkus.runtime.util.StringUtil.join;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.lowerCaseFirst;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;

public class PSIQuarkusManager {
    public static final PSIQuarkusManager INSTANCE = new PSIQuarkusManager();
    private static final List<String> NUMBER_TYPES = Arrays.asList("short", "int", "long", "double", "float");

    private static final Logger LOGGER = LoggerFactory.getLogger(PSIQuarkusManager.class);

    private static Module getModule(VirtualFile file) {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
            if (module != null) {
                return module;
            }
        }
        return null;
    }

    private static VirtualFile uriToVirtualFile(String uri) throws URISyntaxException {
        return LocalFileSystem.getInstance().findFileByIoFile(Paths.get(new URI(uri)).toFile());
    }

    private static Query<PsiMember> getQuery(String annotationFQCN, Module module, QuarkusPropertiesScope scope, boolean isTest, List<VirtualFile> deploymentFiles) {
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(module.getProject());
        PsiClass serviceAnnotation = javaPsiFacade.findClass(annotationFQCN, GlobalSearchScope.allScope(module.getProject()));
        if (serviceAnnotation != null) {
            return AnnotatedElementsSearch.searchElements(serviceAnnotation, (SearchScope) getPSIScope(module, scope, isTest, deploymentFiles), PsiMember.class);
        } else {
            return new EmptyQuery<>();
        }
    }

    @NotNull
    private static GlobalSearchScope getPSIScope(Module module, QuarkusPropertiesScope scope, boolean isTest, List<VirtualFile> deploymentFiles) {
        return scope== QuarkusPropertiesScope.sources?module.getModuleScope(isTest):module.getModuleScope(isTest).union(module.getModuleWithLibrariesScope());
    }

    public List<ExtendedConfigDescriptionBuildItem> getConfigItems(QuarkusProjectInfoParams request) {
        long start = System.currentTimeMillis();
        try {
            VirtualFile file = uriToVirtualFile(request.getUri());
            Module module = getModule(file);
            boolean isTest = ModuleRootManager.getInstance(module).getFileIndex().isInTestSourceContent(file);
            return getConfigItems(module, request.getScope(), isTest);
        } catch (URISyntaxException e) {
            return Collections.emptyList();
        } finally {
            LOGGER.info("getConfigItems duration= " + (System.currentTimeMillis() - start));
        }
    }

    @NotNull
    public List<ExtendedConfigDescriptionBuildItem> getConfigItems(Module module, QuarkusPropertiesScope scope, boolean isTest) {
        List<ExtendedConfigDescriptionBuildItem> configItems = new ArrayList<>();
        Map<VirtualFile, Properties> javaDocCache = new HashMap<>();
        if (module != null) {
            List<VirtualFile> deploymentFiles = ToolDelegate.scanDeploymentFiles(module);

            DumbService.getInstance(module.getProject()).runReadActionInSmartMode(() -> {
                Query<PsiMember> query = new MergeQuery<>(getQuery(CONFIG_ROOT_ANNOTATION, module, scope, isTest, deploymentFiles),
                        getQuery(CONFIG_PROPERTY_ANNOTATION, module, scope, isTest, deploymentFiles));
                query.forEach(psiMember -> {
                    process(psiMember, javaDocCache, configItems);
                });
            });
        }
        return configItems;
    }

    private void process(PsiMember psiMember, Map<VirtualFile, Properties> javaDocCache, List<ExtendedConfigDescriptionBuildItem> configItems) {
        for(PsiAnnotation annotation : psiMember.getAnnotations()) {
            if (annotation.getQualifiedName().equals(CONFIG_ROOT_ANNOTATION)) {
                processConfigRoot(annotation, psiMember, javaDocCache, configItems);
            } else if (annotation.getQualifiedName().equals(CONFIG_PROPERTY_ANNOTATION)) {
                processConfigProperty(annotation, psiMember, javaDocCache, configItems);
            }
        }
    }


    private void processConfigRoot(PsiAnnotation configRootAnnotation, PsiMember psiMember, Map<VirtualFile, Properties> javaDocCache, List<ExtendedConfigDescriptionBuildItem> configItems) {
        ConfigPhase configPhase = getConfigPhase(configRootAnnotation);
        String configRootAnnotationName = getConfigRootName(configRootAnnotation);
        String extension = getExtensionName(getSimpleName(psiMember), configRootAnnotationName, configPhase);
        if (extension == null) {
            return;
        }
        // Location (JAR, src)
        VirtualFile packageRoot = getRootDirectory(PsiTreeUtil.getParentOfType(psiMember, PsiFile.class));
        String location = getLocation(psiMember.getProject(), packageRoot);
        String extensionName = getExtensionName(location);
        String baseKey = QUARKUS_PREFIX + extension;
        processConfigGroup(location, extensionName, psiMember, baseKey, configPhase, javaDocCache, configItems);
    }

    private static String getLocation(Project project, VirtualFile directory) {
        String location = null;
        Module module = ProjectFileIndex.getInstance(project).getModuleForFile(directory);
        if (module != null) {
            VirtualFile moduleRoot = LocalFileSystem.getInstance().findFileByIoFile(new File(module.getModuleFilePath()).getParentFile());
            String path = VfsUtilCore.getRelativePath(directory, moduleRoot);
            if (path != null) {
                location = '/' + module.getName() + '/' + path;
            }
        }
        if (location == null) {
            location = directory.getPath();
        }
        if (location.endsWith("!/")) {
            location = location.substring(0, location.length() - 2);
        }
        return location;
    }

    private String getExtensionName(String location) {
        if (location == null) {
            return null;
        }
        if (!location.endsWith(".jar")) {
            return null;
        }
        int start = location.lastIndexOf('/');
        start++;
        int end = location.lastIndexOf('-');
        if (end == -1) {
            end = location.lastIndexOf('.');
        }
        if (end < start) {
            return null;
        }
        String extensionName = location.substring(start, end);
        if (extensionName.endsWith("-deployment")) {
            extensionName = extensionName.substring(0, extensionName.length() - "-deployment".length());
        }
        return extensionName;
    }

    private void processConfigGroup(String location, String extensionName, PsiMember psiMember, String baseKey, ConfigPhase configPhase, Map<VirtualFile, Properties> javaDocCache, List<ExtendedConfigDescriptionBuildItem> configItems) {
        if (psiMember instanceof PsiClass) {
            for(PsiField field : ((PsiClass)psiMember).getAllFields()) {
                PsiFile f = PsiTreeUtil.getParentOfType(field, PsiFile.class);
                VirtualFile dir = getRootDirectory(f);
                final PsiAnnotation configItemAnnotation = getAnnotation((PsiModifierListOwner) field,
                        CONFIG_ITEM_ANNOTATION);
                String name = configItemAnnotation == null ? hyphenate(field.getName())
                        : getAnnotationMemberValue(configItemAnnotation, "name");
                if (name == null) {
                    name = ConfigItem.HYPHENATED_ELEMENT_NAME;
                }
                String subKey;
                boolean consume;
                if (name.equals(ConfigItem.PARENT)) {
                    subKey = baseKey;
                    consume = false;
                } else if (name.equals(ConfigItem.ELEMENT_NAME)) {
                    subKey = baseKey + "." + field.getName();
                    consume = true;
                } else if (name.equals(ConfigItem.HYPHENATED_ELEMENT_NAME)) {
                    subKey = baseKey + "." + hyphenate(field.getName());
                    consume = true;
                } else {
                    subKey = baseKey + "." + name;
                    consume = true;
                }
                final String defaultValue = configItemAnnotation == null ? ConfigItem.NO_DEFAULT
                        : getAnnotationMemberValue(configItemAnnotation, "defaultValue");

                String fieldTypeName = getResolvedTypeName(field);
                /*IType fieldClass = findType(field.getJavaProject(), fieldTypeName);*/
                PsiClass fieldClass = JavaPsiFacade.getInstance(field.getProject()).findClass(fieldTypeName, GlobalSearchScope.allScope(field.getProject()));
                final PsiAnnotation configGroupAnnotation = getAnnotation((PsiModifierListOwner) fieldClass,
                        CONFIG_GROUP_ANNOTATION);
                if (configGroupAnnotation != null) {
                    processConfigGroup(location, extensionName, fieldClass, subKey, configPhase,
                            javaDocCache, configItems);
                } else {
                    addField(location, extensionName, field, fieldTypeName, fieldClass, subKey, defaultValue,
                            configPhase, javaDocCache, configItems);
                }
            }
        }
    }

    private void processConfigProperty(PsiAnnotation annotation, PsiMember psiMember, Map<VirtualFile, Properties> javaDocCache, List<ExtendedConfigDescriptionBuildItem> configItems) {
        if (psiMember instanceof PsiField || psiMember instanceof PsiMethod) {
            String propertyName = getAnnotationMemberValue(annotation, "name");
            if (StringUtils.isNotEmpty(propertyName)) {
                String propertyTypeName;
                if (psiMember instanceof PsiField) {
                    propertyTypeName = getResolvedTypeName((PsiField)psiMember);
                } else {
                    propertyTypeName = getResolvedTypeName((PsiMethod)psiMember);
                }
                PsiClass fieldClass = JavaPsiFacade.getInstance(psiMember.getProject()).findClass(propertyTypeName, GlobalSearchScope.allScope(psiMember.getProject()));
                String defaultValue = getAnnotationMemberValue(annotation, "defaultValue");
                VirtualFile packageRoot = getRootDirectory(PsiTreeUtil.getParentOfType(psiMember, PsiFile.class));
                String location = getLocation(psiMember.getProject(), packageRoot);
                String extensionName = getExtensionName(location);
                addField(location, extensionName, (PsiField)psiMember, propertyTypeName, fieldClass, propertyName, defaultValue,
                        null, javaDocCache, configItems);

            }
        }
    }

    private VirtualFile getRootDirectory(PsiFile f) {
        ProjectFileIndex index = ProjectFileIndex.getInstance(f.getProject());
        VirtualFile directory = index.getSourceRootForFile(f.getVirtualFile());
        if (directory == null) {
            directory = index.getClassRootForFile(f.getVirtualFile());
        }
        return directory;
    }

    private void addField(String location, String extensionName, PsiField field, String fieldTypeName, PsiClass fieldClass, String propertyName, String defaultValue, ConfigPhase configPhase, Map<VirtualFile, Properties> javaDocCache, List<ExtendedConfigDescriptionBuildItem> configItems) {
        // Class type
        String type = fieldClass != null ? ClassUtil.getJVMClassName(fieldClass) : fieldTypeName;

        // Javadoc
        String docs = getJavadoc(field, javaDocCache);
        //docs = converter.convert(docs);

        // field and class source
        String source = ClassUtil.getJVMClassName(field.getContainingClass()) + "#" + field.getName();

        // Enumerations
        List<EnumItem> enumerations = getEnumerations(fieldClass);

        // Default value for primitive type
        if ("boolean".equals(fieldTypeName)) {
            addField(propertyName, type, ConfigItem.NO_DEFAULT.equals(defaultValue) ? "propertyNamefalse" : defaultValue, docs,
                    location, extensionName, source, enumerations, configPhase, configItems);
        } else if (isNumber(fieldTypeName)) {
            addField(propertyName, type, ConfigItem.NO_DEFAULT.equals(defaultValue) ? "0" : defaultValue, docs,
                    location, extensionName, source, enumerations, configPhase, configItems);
        } else if (isMap(fieldTypeName)) {
            // FIXME: find better mean to check field is a Map
            // this code works only if user uses Map as declaration and not if they declare
            // HashMap for instance
            String[] rawTypeParameters = getRawTypeParameters(fieldTypeName);
            if ((rawTypeParameters[0].trim().equals("java.lang.String"))) {
                // The key Map must be a String
                processMap(field, propertyName, rawTypeParameters[1], docs, location, extensionName, source, configPhase,
                        javaDocCache, configItems);
            }
        } else if (isList(fieldTypeName)) {
            addField(propertyName, type, defaultValue, docs, location, extensionName, source, enumerations, configPhase,
                    configItems);
        } else if (isOptional(fieldTypeName)) {
            ExtendedConfigDescriptionBuildItem item = addField(propertyName, type, defaultValue, docs, location, extensionName, source,
                    enumerations, configPhase, configItems);
            item.setRequired(false);
        } else {
            addField(propertyName, type, defaultValue, docs, location, extensionName, source, enumerations, configPhase,
                    configItems);
        }
    }

    private void processMap(PsiField field, String baseKey, String mapValueClass, String docs, String location, String extensionName, String source, ConfigPhase configPhase, Map<VirtualFile, Properties> javaDocCache, List<ExtendedConfigDescriptionBuildItem> configItems) {
        final String subKey = baseKey + ".{*}";
        if ("java.util.Map".equals(mapValueClass)) {
            // ignore, Map must be parameterized
        } else if (isMap(mapValueClass)) {
            String[] rawTypeParameters = getRawTypeParameters(mapValueClass);
            processMap(field, subKey, rawTypeParameters[1], docs, location, extensionName, source, configPhase,
                    javaDocCache, configItems);
        } else if (isOptional(mapValueClass)) {
            // Optionals are not allowed as a map value type
        } else {
            PsiClass type = findType(field.getManager(), mapValueClass);
            if (type == null || isPrimitiveType(mapValueClass)) {
                // This case comes from when mapValueClass is:
                // - Simple type, like java.lang.String
                // - Type which cannot be found (bad classpath?)
                addField(location, extensionName, field, mapValueClass, null, subKey, null, configPhase, javaDocCache,
                        configItems);
            } else {
                processConfigGroup(location, extensionName, type, subKey, configPhase, javaDocCache, configItems);
            }
        }
    }

    private PsiClass findType(PsiManager manager, String mapValueClass) {
        JavaPsiFacade facade = JavaPsiFacade.getInstance(manager.getProject());
        return facade.findClass(mapValueClass, GlobalSearchScope.allScope(manager.getProject()));
    }

    private ExtendedConfigDescriptionBuildItem addField(String propertyName, String type, String defaultValue, String docs, String location, String extensionName, String source, List<EnumItem> enums, ConfigPhase configPhase, List<ExtendedConfigDescriptionBuildItem> configItems) {
        ExtendedConfigDescriptionBuildItem property = new ExtendedConfigDescriptionBuildItem();
        property.setPropertyName(propertyName);
        property.setType(type);
        property.setDefaultValue(defaultValue);
        property.setDocs(docs);

        // Extra properties
        property.setExtensionName(extensionName);
        property.setLocation(location);
        property.setSource(source);
        if (configPhase != null) {
            property.setPhase(getPhase(configPhase));
        }
        property.setEnums(enums);

        configItems.add(property);
        return property;
    }

    private static int getPhase(ConfigPhase configPhase) {
        switch (configPhase) {
            case BUILD_AND_RUN_TIME_FIXED:
                return ExtendedConfigDescriptionBuildItem.CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED;
            case RUN_TIME:
                return ExtendedConfigDescriptionBuildItem.CONFIG_PHASE_RUN_TIME;
            default:
                return ExtendedConfigDescriptionBuildItem.CONFIG_PHASE_BUILD_TIME;
        }
    }

    private String[] getRawTypeParameters(String fieldTypeName) {
        int start = fieldTypeName.indexOf("<") + 1;
        int end = fieldTypeName.lastIndexOf(">");
        String keyValue = fieldTypeName.substring(start, end);
        int index = keyValue.indexOf(',');
        return new String[] { keyValue.substring(0, index), keyValue.substring(index + 1, keyValue.length()) };
    }

    private boolean isOptional(String fieldTypeName) {
        return fieldTypeName.startsWith("java.util.Optional<");
    }

    private boolean isList(String valueClass) {
        return valueClass.startsWith("java.util.List<");
    }

    private boolean isMap(String mapValueClass) {
        return mapValueClass.startsWith("java.util.Map<");
    }

    private boolean isNumber(String valueClass) {
        return NUMBER_TYPES.contains(valueClass);
    }

    private static boolean isPrimitiveType(String valueClass) {
        return valueClass.equals("java.lang.String") || valueClass.equals("java.lang.Boolean")
                || valueClass.equals("java.lang.Integer") || valueClass.equals("java.lang.Long")
                || valueClass.equals("java.lang.Double") || valueClass.equals("java.lang.Float");
    }

    private List<EnumItem> getEnumerations(PsiClass fieldClass) {
        List<EnumItem> enumerations = null;
        if (fieldClass != null && fieldClass.isEnum()) {
            enumerations = new ArrayList<>();
            PsiElement[] children = fieldClass.getChildren();
            for (PsiElement c : children) {
                if (c instanceof PsiEnumConstant) {
                    String enumName = ((PsiEnumConstant) c).getName();
                    //TODO: implement enum item docs
                    enumerations.add(new EnumItem(enumName, null));
                }
            }
        }
        return enumerations;

    }

    private String getJavadoc(PsiField field, Map<VirtualFile, Properties> javaDocCache) {
        VirtualFile packageRoot = getRootDirectory(PsiTreeUtil.getParentOfType(field, PsiFile.class));
        Properties properties = javaDocCache.get(packageRoot);
        if (properties == null) {
            properties = new Properties();
            javaDocCache.put(packageRoot, properties);
            String quarkusJavadocResource = findJavadocFromQuakusJavadocProperties(packageRoot);
            if (quarkusJavadocResource != null) {
                try (Reader reader = new StringReader(quarkusJavadocResource)) {
                    properties.load(reader);
                } catch (Exception e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
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

    private String findJavadocFromQuakusJavadocProperties(VirtualFile packageRoot) {
        VirtualFile metaInfDir = packageRoot.findChild("META-INF");
        if (metaInfDir != null) {
            VirtualFile file = metaInfDir.findChild(QUARKUS_JAVADOC_PROPERTIES);
            if (file != null) {
                try {
                    return VfsUtilCore.loadText(file);
                } catch (IOException e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                }
            }
        }
        return null;
    }

    private String getResolvedTypeName(PsiField field) {
        return field.getType().getCanonicalText();
    }

    private String getResolvedTypeName(PsiMethod method) {
        return method.getReturnType().getCanonicalText();
    }

    private PsiAnnotation getAnnotation(PsiModifierListOwner annotatable, String annotationName) {
        if (annotatable == null) {
            return null;
        }
        PsiAnnotation[] annotations = annotatable.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if (annotationName.equals(annotation.getQualifiedName())) {
                return annotation;
            }
        }
        return null;
    }

    private String getExtensionName(String configRootClassSimpleName, String configRootAnnotationName, ConfigPhase configPhase) {
        // See
        // https://github.com/quarkusio/quarkus/blob/master/core/deployment/src/main/java/io/quarkus/deployment/configuration/ConfigDefinition.java#L173
        // registerConfigRoot
        final String containingName;
        if (configPhase == ConfigPhase.RUN_TIME) {
            containingName = join(withoutSuffix(lowerCaseFirst(camelHumpsIterator(configRootClassSimpleName)), "Config",
                    "Configuration", "RunTimeConfig", "RunTimeConfiguration"));
        } else {
            containingName = join(withoutSuffix(lowerCaseFirst(camelHumpsIterator(configRootClassSimpleName)), "Config",
                    "Configuration", "BuildTimeConfig", "BuildTimeConfiguration"));
        }
        final String name = configRootAnnotationName;
        final String rootName;
        if (name.equals(ConfigItem.PARENT)) {
            // throw reportError(configRoot, "Root cannot inherit parent name because it has
            // no parent");
            return null;
        } else if (name.equals(ConfigItem.ELEMENT_NAME)) {
            rootName = containingName;
        } else if (name.equals(ConfigItem.HYPHENATED_ELEMENT_NAME)) {
            rootName = join("-",
                    withoutSuffix(lowerCase(camelHumpsIterator(configRootClassSimpleName)), "config", "configuration"));
        } else {
            rootName = name;
        }
        return rootName;
    }

    private String getSimpleName(PsiMember psiMember) {
        String elementName = psiMember.getName();
        int index = elementName.lastIndexOf('.');
        return index != -1 ? elementName.substring(index + 1, elementName.length()) : elementName;
    }

    private String getConfigRootName(PsiAnnotation configRootAnnotation) {
        String value = getAnnotationMemberValue(configRootAnnotation, "name");
        if (value != null) {
            return value;
        }
        return ConfigItem.HYPHENATED_ELEMENT_NAME;
    }

    private ConfigPhase getConfigPhase(PsiAnnotation annotation) {
        String value = getAnnotationMemberValue(annotation, "phase");
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

    private String getAnnotationMemberValue(PsiAnnotation annotation, String memberName) {
        PsiAnnotationMemberValue member = annotation.findDeclaredAttributeValue(memberName);
        String value = member != null && member.getText() != null ? member.getText() : null;
        if (value != null && value.length() > 1 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }
}
