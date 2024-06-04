/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.qute.psi.internal.AnnotationLocationSupport;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.qute.psi.utils.TemplatePathInfo;
import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.*;
import static com.redhat.devtools.intellij.qute.psi.internal.template.datamodel.CheckedTemplateSupport.getBasePath;
import static com.redhat.devtools.intellij.qute.psi.internal.template.datamodel.CheckedTemplateSupport.isIgnoreFragments;
import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.*;

/**
 * Abstract class which collects {@link PsiMethod} or
 * {@link com.intellij.psi.PsiField} which defines a Qute template link:
 *
 * <ul>
 * <li>declared methods which have class annotated with @CheckedTemplate.</li>
 * <li>declared field which have Template as type.</li>
 * </ul>
 *
 * @author Angelo ZERR
 */
public abstract class AbstractQuteTemplateLinkCollector extends JavaRecursiveElementVisitor {

    private static final Logger LOGGER = Logger.getLogger(AbstractQuteTemplateLinkCollector.class.getName());

    private static String[] suffixes = {".qute.html", ".qute.json", ".qute.txt", ".qute.yaml", ".html", ".json",
            ".txt", ".yaml"};

    protected static final String PREFERRED_SUFFIX = ".html"; // TODO make it configurable

    protected final PsiFile typeRoot;
    protected final IPsiUtils utils;
    protected final ProgressIndicator monitor;
    private final @Nullable VirtualFile templatesDir;
    private final String relativeTemplatesBaseDir;
    private int levelTypeDecl;

    private AnnotationLocationSupport annotationLocationSupport;

    private PsiFile compilationUnit;

    public AbstractQuteTemplateLinkCollector(PsiFile typeRoot, IPsiUtils utils, ProgressIndicator monitor) {
        this.typeRoot = typeRoot;
        this.compilationUnit = typeRoot;
        this.utils = utils;
        this.monitor = monitor;
        this.levelTypeDecl = 0;
        VirtualFile resourcesDir = findBestResourcesDir(utils.getModule());
        this.templatesDir = resourcesDir != null ? resourcesDir.findFileByRelativePath(TEMPLATES_FOLDER_NAME) : null;
        this.relativeTemplatesBaseDir = PsiQuteProjectUtils.getRelativeTemplateBaseDir(utils.getModule(), resourcesDir);
    }

    @Override
    public void visitField(PsiField node) {
        PsiType type = node.getType();
        if (isTemplateType(type)) {
            // The field type is the Qute template
            // private Template items;

            // Try to get the @Location annotation
            // @Location("detail/items2_v1.html")
            // Template items2;
            PsiLiteralValue locationExpression = AnnotationLocationSupport.getLocationExpression(node, node.getModifierList());

            if (locationExpression == null) {
                // The field doesn't declare @Location,
                // try to find the @Location declared in the constructor parameter which
                // initializes the field

                // private final Template page;
                // public SomePage(@Location("foo/bar/page.qute.html") Template page) {
                // this.page = requireNonNull(page, "page is required");
                // }
                locationExpression = getAnnotationLocationSupport()
                        .getLocationExpressionFromConstructorParameter(node.getName());
            }
            String fieldName = node.getName();
            collectTemplateLink(null, node, locationExpression, getTypeDeclaration(node), null, fieldName, false);
        }
        super.visitField(node);
    }

    /**
     * Returns the @Location support.
     *
     * @return the @Location support.
     */
    private AnnotationLocationSupport getAnnotationLocationSupport() {
        if (annotationLocationSupport == null) {
            // Initialize the @Location support to try to find an @Location in the
            // constructor which initializes some fields
            annotationLocationSupport = new AnnotationLocationSupport(compilationUnit);
        }
        return annotationLocationSupport;
    }

    @Override
    public void visitClass(PsiClass node) {
        levelTypeDecl++;
        for (PsiAnnotation annotation : node.getAnnotations()) {
            if (AnnotationUtils.isMatchAnnotation(annotation, CHECKED_TEMPLATE_ANNOTATION)
                    || AnnotationUtils.isMatchAnnotation(annotation, OLD_CHECKED_TEMPLATE_ANNOTATION)) {
                // @CheckedTemplate
                // public static class Templates {
                // public static native TemplateInstance book(Book book);
                boolean ignoreFragments = isIgnoreFragments(annotation);
                String basePath = getBasePath(annotation);
                for (PsiMethod method : node.getMethods()) {
                    collectTemplateLink(basePath, method, node, ignoreFragments);
                }
            }
        }
        super.visitClass(node);
        levelTypeDecl--;
    }

    private static PsiClass getTypeDeclaration(PsiElement node) {
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }


    private void collectTemplateLink(String basePath, PsiMethod methodDeclaration, PsiClass type, boolean ignoreFragments) {
        String className = null;
        boolean innerClass = levelTypeDecl > 1;
        if (innerClass) {
            className = PsiTypeUtils.getSimpleClassName(typeRoot.getName());
        }
        String methodName = methodDeclaration.getName();
        collectTemplateLink(basePath, methodDeclaration, null, type, className, methodName, ignoreFragments);
    }

    private void collectTemplateLink(String basePath, PsiElement fieldOrMethod, PsiLiteralValue locationAnnotation, PsiClass type, String className,
                                     String fieldOrMethodName, boolean ignoreFragment) {
        try {
            String location = locationAnnotation != null && locationAnnotation.getValue() instanceof String ? (String) locationAnnotation.getValue() : null;
            Module project = utils.getModule();

            TemplatePathInfo templatePathInfo = location != null
                    ? PsiQuteProjectUtils.getTemplatePath(null, basePath, null, location, ignoreFragment)
                    : PsiQuteProjectUtils.getTemplatePath(null, basePath, className, fieldOrMethodName, ignoreFragment);
            String templateUriWithoutBaseDir = templatePathInfo.getTemplateUri();

            boolean definedSuffix = location != null;
            VirtualFile templateFile = getTemplateFile(templateUriWithoutBaseDir, !definedSuffix);
            String templateUri = getRelativePath(templateUriWithoutBaseDir, definedSuffix, templateFile, project);

            templatePathInfo = new TemplatePathInfo(templateUri, templatePathInfo.getFragmentId());
            collectTemplateLink(basePath, fieldOrMethod, locationAnnotation, type, className, fieldOrMethodName, location,
                    templateFile, templatePathInfo);
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Error while creating Qute CodeLens for Java file.", e);
        }
    }

    /**
     * Returns the relative template file path from the module.
     *
     * @param templateFilePath the template file path (without file extension).
     * @param templateFile     the template file and null otherwise.
     * @param module           the module.
     * @return the relative template file path from the module.
     */
    private @NotNull String getRelativePath(@NotNull String templateFilePath, boolean definedSuffix, @Nullable VirtualFile templateFile, @NotNull Module module) {
        if (templateFile != null) {
            // The template file exists
            // Compute the relative path from the module root path:
            // - src/main/resources/templates/page.html for Maven project
            // - resources/templates/page.html for Gradle project
            VirtualFile @NotNull [] roots = getContentRoots(module);
            for (VirtualFile root : roots) {
                String path = VfsUtilCore.getRelativePath(templateFile, root);
                if (path != null) {
                    return path;
                }
            }
            // Corner usecase, returns the full template file path.
            return templateFile.getPath();
        }
        // The template file doesn't exist
        // Compute the relative path from the module root path:
        // - src/main/resources/templates/page.html for Maven project
        // - resources/templates/page.html for Gradle project
        return relativeTemplatesBaseDir + templateFilePath + (definedSuffix ? "" : PREFERRED_SUFFIX);
    }


    private @Nullable VirtualFile getTemplateFile(String templateUri, boolean discoverSuffix) {
        if (templatesDir == null || !templatesDir.exists()) {
            return null;
        }
        if (!discoverSuffix) {
            return templatesDir.findFileByRelativePath(templateUri);
        }

        for (String suffix : suffixes) {
            VirtualFile templateFile = templatesDir.findFileByRelativePath(templateUri + suffix);
            if (templateFile != null && templateFile.exists()) {
                return templateFile;
            }
        }
        return templatesDir.findFileByRelativePath(templateUri + PREFERRED_SUFFIX);
    }

    protected static String getVirtualFileUrl(Module project, String templateFilePath) {
        try {
            for (VirtualFile root : getContentRoots(project)) {
                return new URL(LSPIJUtils.toUri(root).toURL(), templateFilePath).toString();
            }
        } catch (MalformedURLException e) {
        }
        return null;
    }

    protected Range createRange(PsiElement fieldOrMethod) {
        if (fieldOrMethod instanceof PsiField) {
            TextRange tr = ((PsiField) fieldOrMethod).getNameIdentifier().getTextRange();
            return utils.toRange(typeRoot, tr.getStartOffset(), tr.getLength());
        }
        if (fieldOrMethod instanceof PsiLiteralValue) {
            TextRange tr = fieldOrMethod.getTextRange();
            return utils.toRange(typeRoot, tr.getStartOffset(), tr.getLength());
        }
        PsiMethod method = (PsiMethod) fieldOrMethod;
        PsiIdentifier methodName = method.getNameIdentifier();
        TextRange tr = methodName.getTextRange();
        return utils.toRange(typeRoot, tr.getStartOffset(), tr.getLength());
    }

    protected abstract void collectTemplateLink(String basePath, PsiElement node, PsiLiteralValue locationAnnotation, PsiClass type,
                                                String className, String fieldOrMethodName, String location, VirtualFile templateFile, TemplatePathInfo templatePathInfo);

    private static boolean isTemplateType(PsiType type) {
        if (type instanceof PsiClassType) {
            PsiClass clazz = ((PsiClassType) type).resolve();
            if (clazz != null) {
                return TEMPLATE_CLASS.equals(clazz.getQualifiedName());
            }
        }
        return false;
    }
}
