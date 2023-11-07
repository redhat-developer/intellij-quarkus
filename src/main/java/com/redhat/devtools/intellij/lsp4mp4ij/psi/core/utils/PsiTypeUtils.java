/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationMethod;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * PSI Type utilities.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/JDTTypeUtils.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/JDTTypeUtils.java</a>
 */

public class PsiTypeUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsiTypeUtils.class);

    private static final List<String> NUMBER_TYPES = Arrays.asList("short", "int", "long", "double", "float");

    public static String getResolvedTypeName(PsiType type) {
        if (type == null) {
            return null;
        }
        while (type instanceof PsiArrayType) {
            type = ((PsiArrayType) type).getComponentType();
        }
        return type.getCanonicalText();
    }

    public static String getResolvedTypeName(PsiField field) {
        return getResolvedTypeName(field.getType());
    }

    public static String getResolvedTypeName(PsiVariable variable) {
        return getResolvedTypeName(variable.getType());
    }

    public static String getResolvedResultTypeName(PsiMethod method) {
        return getResolvedTypeName(method.getReturnType());
    }

    public static String getResolvedTypeName(PsiElement element) {
        if (element instanceof PsiMethod) {
            return getResolvedResultTypeName((PsiMethod) element);
        } else if (element instanceof PsiField) {
            return getResolvedTypeName((PsiField) element);
        } else if (element instanceof PsiVariable) {
            return getResolvedTypeName((PsiVariable) element);
        }
        return null;
    }

    public static String getDefaultValue(PsiMethod method) {
        String value = null;
        if (method instanceof PsiAnnotationMethod) {
            PsiAnnotationMemberValue defaultValue = ((PsiAnnotationMethod) method).getDefaultValue();
            if (defaultValue instanceof PsiAnnotation) {
                value = ((PsiAnnotation) defaultValue).getQualifiedName();
                int index = value.lastIndexOf('.');
                if (index != (-1)) {
                    value = value.substring(index + 1, value.length());
                }
            } else if (defaultValue instanceof PsiLiteral) {
                value = ((PsiLiteral) defaultValue).getValue().toString();
            } else if (defaultValue instanceof PsiReference) {
                value = ((PsiReference) defaultValue).getCanonicalText();
                int index = value.lastIndexOf('.');
                if (index != (-1)) {
                    value = value.substring(index + 1, value.length());
                }
            }
        }
        return value == null || value.isEmpty() ? null : value;
    }


    public static String getPropertyType(PsiClass psiClass, String typeName) {
        return psiClass != null ? psiClass.getQualifiedName() : typeName;
    }

    public static String getSourceType(PsiModifierListOwner psiElement) {
        if (psiElement instanceof PsiField || psiElement instanceof PsiMethod) {
            return ClassUtil.getJVMClassName(((PsiMember) psiElement).getContainingClass());
        } else if (psiElement instanceof PsiParameter) {
            return ClassUtil.getJVMClassName(((PsiMethod) ((PsiParameter) psiElement).getDeclarationScope()).getContainingClass());
        }
        if (psiElement instanceof PsiClass) {
            return getPropertyType((PsiClass) psiElement, null);
        }
        return null;
    }

    public static String getSourceMethod(PsiMethod method) {
        //TODO: check method signature
        return method.getName() + ClassUtil.getAsmMethodSignature(method);
    }


    @Nullable
    public static PsiClass findType(PsiManager manager, String name) {
        return ClassUtil.findPsiClass(manager, name);
    }

    public static PsiClass findType(Module module, String name) {
        return ClassUtil.findPsiClass(PsiManager.getInstance(module.getProject()), name, null, false,
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module));
    }

    public static String getSourceField(PsiMember psiMember) {
        return psiMember.getName();
    }

    public static boolean isBinary(PsiModifierListOwner psiMember) {
        return psiMember instanceof PsiCompiledElement;
    }

    public static boolean isOptional(String fieldTypeName) {
        return fieldTypeName.startsWith("java.util.Optional");
    }

    /**
     * Returns the enclosed type declared in the given <code>typeName</code> and
     * null otherwise.
     *
     * @param typeName
     * @return
     */
    public static String getOptionalTypeParameter(String typeName) {
        if (!isOptional(typeName)) {
            return null;
        }
        int start = typeName.indexOf('<');
        if (start == -1) {
            return null;
        }
        // the type name follows the signature java.util.Optional<MyType>
        // extract the enclosed type MyType.
        int end = typeName.lastIndexOf('>');
        return typeName.substring(start + 1, end);
    }

    /**
     * Returns the first (generic) type parameter for the given <code>psiType</code>.
     * Returns <code>null</code> if the given psiType has no generic type parameter.
     * <p>
     * Examples:
     * <ul>.
     *  <li>returns {@code String} for {@code List<String>}</li>
     *  <li>returns {@code List<String>} for {@code Optional<List<String>>}</li>
     *  <li>returns {@code null} for {@code OptionalInt}</li>
     * </ul>
     *
     * @param psiType the psiType to return the generic type parameter for
     * @return the generic type parameter for the given psiType
     */
    public static PsiType getFirstTypeParameter(PsiType psiType) {
        PsiType type = null;
        PsiClassType classType = getPsiClassType(psiType);
        PsiClass resolvedClassType = classType.resolve();
        if (resolvedClassType != null) {
            PsiTypeParameter[] types = resolvedClassType.getTypeParameters();
            if (types.length > 0) {
                PsiClassType.ClassResolveResult resolved = PsiUtil.resolveGenericsClassInType(psiType);
                Map<PsiTypeParameter, PsiType> substitutions = resolved.getSubstitutor().getSubstitutionMap();
                type = substitutions.get(types[0]);
            }
        }
        return type;
    }

    private static PsiClassType getPsiClassType(PsiType psiType) {
        PsiClass psiClass = PsiTypesUtil.getPsiClass(psiType);
        if (psiClass == null) {
            return null;
        }
        PsiClassType psiClassType = PsiTypesUtil.getClassType(psiClass);
        return psiClassType;
    }

    public static PsiClass getEnclosedType(PsiClass type, String typeName, PsiManager javaProject) {
        // type name is the string of the JDT type (which could be null if type is not
        // retrieved)
        String enclosedType = typeName;
        if (type == null) {
            // JDT type is null, in some case it's because type is optional (ex :
            // java.util.Optional<MyType>)
            // try to extract the enclosed type from the optional type (to get 'MyType' )
            enclosedType = getOptionalTypeParameter(typeName);
            if (enclosedType != null) {
                type = findType(javaProject, enclosedType);
            }
        }
        return type;
    }

    public static boolean isList(String valueClass) {
        return valueClass.startsWith("java.util.List<");
    }

    public static boolean isMap(String mapValueClass) {
        return mapValueClass.startsWith("java.util.Map<");
    }

    public static boolean isNumber(String valueClass) {
        return NUMBER_TYPES.contains(valueClass);
    }

    public static boolean isPrimitiveBoolean(String valueClass) {
        return valueClass.equals("boolean");
    }

    public static boolean isPrimitiveType(String valueClass) {
        return valueClass.equals("java.lang.String") || valueClass.equals("java.lang.Boolean")
                || valueClass.equals("java.lang.Integer") || valueClass.equals("java.lang.Long")
                || valueClass.equals("java.lang.Double") || valueClass.equals("java.lang.Float");
    }

    public static String findPropertiesResource(VirtualFile packageRoot, String propertiesFileName) {
        VirtualFile metaInfDir = packageRoot.findChild("META-INF");
        if (metaInfDir != null) {
            VirtualFile file = metaInfDir.findChild(propertiesFileName);
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


    public static boolean isSimpleFieldType(PsiClass type, String typeName) {
        return type == null || isPrimitiveType(typeName) || isList(typeName) || isMap(typeName) || isOptional(typeName)
                || (type != null && type.isEnum());
    }

    public static VirtualFile getRootDirectory(PsiElement element) {
        return getRootDirectory(PsiTreeUtil.getParentOfType(element, PsiFile.class));
    }

    public static VirtualFile getRootDirectory(PsiFile file) {
        ProjectFileIndex index = ProjectFileIndex.getInstance(file.getProject());
        VirtualFile directory = index.getSourceRootForFile(file.getVirtualFile());
        if (directory == null) {
            directory = index.getClassRootForFile(file.getVirtualFile());
        }
        return directory;
    }

    public static String getLocation(Project project, VirtualFile directory) {
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

    public static boolean overlaps(TextRange typeRange, TextRange methodRange) {
        if (typeRange == null || methodRange == null) {
            return false;
        }
        // method range is overlapping if it appears before or actually overlaps the
        // type's range
        return methodRange.getStartOffset() < typeRange.getStartOffset() || methodRange.getStartOffset() >= typeRange.getStartOffset()
                && methodRange.getStartOffset() <= (typeRange.getStartOffset() + typeRange.getLength());
    }

    public static String getRawResolvedTypeName(PsiType type) {
        if (type == null) {
            return null;
        }
        if (type instanceof PsiClassType) {
            type = ((PsiClassType) type).rawType();
        }
        return type.getCanonicalText();
    }

    public static String getRawResolvedTypeName(PsiMethod method) {
        return getRawResolvedTypeName(method.getReturnType());
    }

    /**
     * Return true if method returns `void`, and false otherwise
     *
     * @param method the method to check return value of
     * @return
     */
    public static boolean isVoidReturnType(PsiMethod method) {
        return PsiType.VOID.equals(method.getReturnType());
    }
}
