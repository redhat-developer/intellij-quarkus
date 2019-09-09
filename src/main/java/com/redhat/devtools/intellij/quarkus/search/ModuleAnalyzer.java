package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.EmptyQuery;
import com.intellij.util.Query;
import com.redhat.quarkus.commons.ExtendedConfigDescriptionBuildItem;
import com.redhat.quarkus.commons.QuarkusProjectInfoParams;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;

import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.*;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_PREFIX;
import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.hyphenate;
import static io.quarkus.runtime.util.StringUtil.join;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.lowerCaseFirst;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;

public class ModuleAnalyzer {
    public static final ModuleAnalyzer INSTANCE = new ModuleAnalyzer();
    private static final List<String> NUMBER_TYPES = Arrays.asList("short", "int", "long", "double", "float");

    private static Module getModule(String uri) {
        try {
            VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(Paths.get(new URI(uri)).toFile());
            for(Project project : ProjectManager.getInstance().getOpenProjects()) {
                Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
                if (module != null) {
                    return module;
                }
            }
        } catch (URISyntaxException e) {}
        return null;
    }

    private static Query<PsiClass> getQuery(String annotationFQCN, Module module) {
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(module.getProject());
        PsiClass serviceAnnotation = javaPsiFacade.findClass(annotationFQCN, GlobalSearchScope.allScope(module.getProject()));
        if (serviceAnnotation != null) {
            return AnnotatedElementsSearch.searchPsiClasses(serviceAnnotation, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module));
        } else {
            return new EmptyQuery<>();
        }
    }

    public List<ExtendedConfigDescriptionBuildItem> getConfigItem(QuarkusProjectInfoParams request) {
        List<ExtendedConfigDescriptionBuildItem> configItems = new ArrayList<>();
        Module module = getModule(request.getUri());
        Map<PsiDirectory, Properties> javaDocCache = new HashMap<>();
        if (module != null) {
            getQuery(CONFIG_ROOT_ANNOTATION, module).forEach(psiClass -> {
                process(psiClass, javaDocCache, configItems);
            });
        }
        return configItems;

    }

    private void process(PsiClass psiClass, Map<PsiDirectory, Properties> javaDocCache, List<ExtendedConfigDescriptionBuildItem> configItems) {
        for(PsiAnnotation annotation : psiClass.getAnnotations()) {
            if (annotation.getQualifiedName().equals(CONFIG_ROOT_ANNOTATION)) {
                processConfigRoot(annotation, psiClass, javaDocCache, configItems);
            }
        }
    }

    private void processConfigRoot(PsiAnnotation configRootAnnotation, PsiClass psiClass, Map<PsiDirectory, Properties> javaDocCache, List<ExtendedConfigDescriptionBuildItem> configItems) {
        ConfigPhase configPhase = getConfigPhase(configRootAnnotation);
        String configRootAnnotationName = getConfigRootName(configRootAnnotation);
        String extension = getExtensionName(getSimpleName(psiClass), configRootAnnotationName, configPhase);
        if (extension == null) {
            return;
        }
        String baseKey = QUARKUS_PREFIX + extension;
        processConfigGroup(psiClass, baseKey, configPhase, javaDocCache, configItems);
    }

    private void processConfigGroup(PsiClass psiClass, String baseKey, ConfigPhase configPhase, Map<PsiDirectory, Properties> javaDocCache, List<ExtendedConfigDescriptionBuildItem> configItems) {
        for(PsiField field : psiClass.getAllFields()) {
            PsiFile f = PsiTreeUtil.getParentOfType(field, PsiFile.class);
            PsiDirectory dir = getRootDirectory(f);
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
            PsiClass fieldClass = PsiTypesUtil.getPsiClass(field.getType());
            final PsiAnnotation configGroupAnnotation = getAnnotation((PsiModifierListOwner) fieldClass,
                    CONFIG_GROUP_ANNOTATION);
            if (configGroupAnnotation != null) {
                processConfigGroup(fieldClass, subKey, configPhase,
                        javaDocCache, configItems);
            } else {
                addField(field, fieldTypeName, fieldClass, subKey, defaultValue,
                        configPhase, javaDocCache, configItems);
            }
        }
    }

    private PsiDirectory getRootDirectory(PsiFile f) {
        PsiDirectory dir = f.getContainingDirectory();
        while (dir != null) {
            if (dir.getName().endsWith(".jar")) {
                break;
            }
            dir = dir.getParent();
        }
        return dir;
    }

    private void addField(PsiField field, String fieldTypeName, PsiClass fieldClass, String propertyName, String defaultValue, ConfigPhase configPhase, Map<PsiDirectory, Properties> javaDocCache, List<ExtendedConfigDescriptionBuildItem> configItems) {
        // Class type
        String type = fieldClass != null ? fieldClass.getQualifiedName() : fieldTypeName;

        // Javadoc
        String docs = getJavadoc(field, javaDocCache);
        //docs = converter.convert(docs);

        // Location (JAR, src)
        PsiDirectory packageRoot = getRootDirectory(PsiTreeUtil.getParentOfType(field, PsiFile.class));
        String location = packageRoot.getName();

        // field and class source
        String source = field.getContainingClass().getQualifiedName() + "#" + field.getName();

        // Enumerations
        List<String> enumerations = getEnumerations(fieldClass);

        // Default value for primitive type
        if ("boolean".equals(fieldTypeName)) {
            addField(propertyName, type, ConfigItem.NO_DEFAULT.equals(defaultValue) ? "propertyNamefalse" : defaultValue, docs,
                    location, source, enumerations, configPhase, configItems);
        } else if (isNumber(fieldTypeName)) {
            addField(propertyName, type, ConfigItem.NO_DEFAULT.equals(defaultValue) ? "0" : defaultValue, docs,
                    location, source, enumerations, configPhase, configItems);
        } else if (isMap(fieldTypeName)) {
            // FIXME: find better mean to check field is a Map
            // this code works only if user uses Map as declaration and not if they declare
            // HashMap for instance
            String[] rawTypeParameters = getRawTypeParameters(fieldTypeName);
            if ((rawTypeParameters[0].trim().equals("java.lang.String"))) {
                // The key Map must be a String
                processMap(field, propertyName, rawTypeParameters[1], docs, location, source, configPhase,
                        javaDocCache, configItems);
            }
        } else if (isList(fieldTypeName)) {
            addField(propertyName, type, defaultValue, docs, location, source, enumerations, configPhase,
                    configItems);
        } else if (isOptional(fieldTypeName)) {
            ExtendedConfigDescriptionBuildItem item = addField(propertyName, type, defaultValue, docs, location, source,
                    enumerations, configPhase, configItems);
            item.setRequired(false);
        } else {
            addField(propertyName, type, defaultValue, docs, location, source, enumerations, configPhase,
                    configItems);
        }
    }

    private void processMap(PsiField field, String baseKey, String mapValueClass, String docs, String location, String source, ConfigPhase configPhase, Map<PsiDirectory, Properties> javaDocCache, List<ExtendedConfigDescriptionBuildItem> configItems) {
        final String subKey = baseKey + ".{*}";
        if ("java.util.Map".equals(mapValueClass)) {
            // ignore, Map must be parameterized
        } else if (isMap(mapValueClass)) {
            String[] rawTypeParameters = getRawTypeParameters(mapValueClass);
            processMap(field, subKey, rawTypeParameters[1], docs, location, source, configPhase,
                    javaDocCache, configItems);
        } else if (isOptional(mapValueClass)) {
            // Optionals are not allowed as a map value type
        } else {
            PsiClass type = findType(field.getManager(), mapValueClass);
            if (type == null) {
                // This case comes from when mapValueClass is:
                // - Simple type, like java.lang.String
                // - Type which cannot be found (bad classpath?)
                addField(field, mapValueClass, null, subKey, null, configPhase, javaDocCache,
                        configItems);
            } else {
                processConfigGroup(type, subKey, configPhase, javaDocCache, configItems);
            }
        }
    }

    private PsiClass findType(PsiManager manager, String mapValueClass) {
        JavaPsiFacade facade = JavaPsiFacade.getInstance(manager.getProject());
        return facade.findClass(mapValueClass, GlobalSearchScope.projectScope(manager.getProject()));
    }

    private ExtendedConfigDescriptionBuildItem addField(String propertyName, String type, String defaultValue, String docs, String location, String source, List<String> enums, ConfigPhase configPhase, List<ExtendedConfigDescriptionBuildItem> configItems) {
        ExtendedConfigDescriptionBuildItem property = new ExtendedConfigDescriptionBuildItem();
        property.setPropertyName(propertyName);
        property.setType(type);
        property.setDefaultValue(defaultValue);
        property.setDocs(docs);

        // Extra properties
        property.setLocation(location);
        property.setSource(source);
        property.setPhase(getPhase(configPhase));
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

    private List<String> getEnumerations(PsiClass fieldClass) {
        List<String> enumerations = null;
        if (fieldClass != null && fieldClass.isEnum()) {
            enumerations = new ArrayList<>();
            PsiElement[] children = fieldClass.getChildren();
            for (PsiElement c : children) {
                if (c instanceof PsiEnumConstant) {
                    String enumName = ((PsiEnumConstant) c).getName();
                    enumerations.add(enumName);
                }
            }
        }
        return enumerations;

    }

    private String getJavadoc(PsiField field, Map<PsiDirectory, Properties> javaDocCache) {
        PsiDirectory packageRoot = getRootDirectory(PsiTreeUtil.getParentOfType(field, PsiFile.class));
        Properties properties = javaDocCache.get(packageRoot);
        if (properties == null) {
            properties = new Properties();
            javaDocCache.put(packageRoot, properties);
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

    private String findJavadocFromQuakusJavadocProperties(PsiDirectory packageRoot) {
        PsiDirectory metaInfDir = packageRoot.findSubdirectory("META-INF");
        if (metaInfDir != null) {
            PsiFile file = metaInfDir.findFile(QUARKUS_JAVADOC_PROPERTIES);
            if (file != null) {
                return file.getText();
            }
        }
        return null;
    }

    private String getResolvedTypeName(PsiField field) {
        PsiClass psiClass = PsiTypesUtil.getPsiClass(field.getType());
        return psiClass != null ? psiClass.getQualifiedName(): "";
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

    private String getSimpleName(PsiClass psiClass) {
        return psiClass.getName();
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
