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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.qute.psi.internal.AnnotationLocationSupport;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.CHECKED_TEMPLATE_ANNOTATION;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.OLD_CHECKED_TEMPLATE_ANNOTATION;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_CLASS;

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
 *
 */
public abstract class AbstractQuteTemplateLinkCollector extends JavaRecursiveElementVisitor {

	private static final Logger LOGGER = Logger.getLogger(AbstractQuteTemplateLinkCollector.class.getName());

	private static String[] suffixes = { ".qute.html", ".qute.json", ".qute.txt", ".qute.yaml", ".html", ".json",
			".txt", ".yaml" };

	protected static final String PREFERRED_SUFFIX = ".html"; // TODO make it configurable

	private static final String TEMPLATE_TYPE = "Template";

	protected final PsiFile typeRoot;
	protected final IPsiUtils utils;
	protected final ProgressIndicator monitor;

	private int levelTypeDecl;

	private AnnotationLocationSupport annotationLocationSupport;

	private PsiFile compilationUnit;

	public AbstractQuteTemplateLinkCollector(PsiFile typeRoot, IPsiUtils utils, ProgressIndicator monitor) {
		this.typeRoot = typeRoot;
		this.compilationUnit = typeRoot;
		this.utils = utils;
		this.monitor = monitor;
		this.levelTypeDecl = 0;
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
				collectTemplateLink(node, locationExpression, getTypeDeclaration(node), null, fieldName);
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
		for(PsiAnnotation annotation : node.getAnnotations()) {
			if (AnnotationUtils.isMatchAnnotation(annotation, CHECKED_TEMPLATE_ANNOTATION)
			|| AnnotationUtils.isMatchAnnotation(annotation, OLD_CHECKED_TEMPLATE_ANNOTATION)) {
				// @CheckedTemplate
				// public static class Templates {
				// public static native TemplateInstance book(Book book);
				for(PsiMethod method : node.getMethods()) {
					collectTemplateLink(method, node);
				}
			}
		}
		super.visitClass(node);
		levelTypeDecl--;
	}

	private static PsiClass getTypeDeclaration(PsiElement node) {
		return PsiTreeUtil.getParentOfType(node, PsiClass.class);
	}


	private void collectTemplateLink(PsiMethod methodDeclaration, PsiClass type) {
		String className = null;
		boolean innerClass = levelTypeDecl > 1;
		if (innerClass) {
			className = PsiTypeUtils.getSimpleClassName(typeRoot.getName());
		}
		String methodName = methodDeclaration.getName();
		collectTemplateLink(methodDeclaration, null, type, className, methodName);
	}

	private void collectTemplateLink(PsiElement fieldOrMethod, PsiLiteralValue locationAnnotation, PsiClass type, String className,
									 String fieldOrMethodName) {
		try {
			String location = locationAnnotation != null && locationAnnotation.getValue() instanceof String ? (String) locationAnnotation.getValue() : null;
			Module project = utils.getModule();
			String templateFilePath = location != null ? PsiQuteProjectUtils.getTemplatePath(null, location)
					: PsiQuteProjectUtils.getTemplatePath(className, fieldOrMethodName);
			VirtualFile templateFile = null;
			if (location == null) {
				templateFile = getTemplateFile(project, templateFilePath);
				templateFilePath = getRelativePath(templateFilePath, templateFile, project);
			} else {
				templateFile = getVirtualFile(project, templateFilePath, "");
			}
			collectTemplateLink(fieldOrMethod, locationAnnotation, type, className, fieldOrMethodName, location,
					templateFile, templateFilePath);
		} catch (RuntimeException e) {
			LOGGER.log(Level.WARNING, "Error while creating Qute CodeLens for Java file.", e);
		}
	}

	private VirtualFile getTemplateFile(Module project, String templateFilePathWithoutExtension) {
		for (String suffix : suffixes) {
			VirtualFile templateFile = getVirtualFile(project, templateFilePathWithoutExtension, suffix);
			if (templateFile != null) {
				return templateFile;
			}
		}
		return getVirtualFile(project, templateFilePathWithoutExtension, PREFERRED_SUFFIX);
	}

	@Nullable
	private VirtualFile getVirtualFile(Module project, String templateFilePathWithoutExtension, String suffix) {
		for(VirtualFile root : ModuleRootManager.getInstance(project).getContentRoots()) {
			VirtualFile templateFile = root.findFileByRelativePath(templateFilePathWithoutExtension + suffix);
			if (templateFile != null && templateFile.exists()) {
				return templateFile;
			}
		}
		return null;
	}

	protected String getVirtualFileUrl(Module project, String templateFilePathWithoutExtension, String suffix) {
		try {
			for(VirtualFile root : ModuleRootManager.getInstance(project).getContentRoots()) {
				return new URL(LSPIJUtils.toUri(root).toURL(), templateFilePathWithoutExtension + suffix).toString();
			}
		} catch (MalformedURLException e) {}
		return null;
	}

	protected String getRelativePath(String templateFilePath, VirtualFile templateFile, Module module) {
		if (templateFile != null) {
			for(VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
				String path = VfsUtilCore.getRelativePath(templateFile, root);
				if (path != null) {
					return path;
				}
			}
			return templateFile.getPath();
		} else {
			return templateFilePath + PREFERRED_SUFFIX;
		}
	}
	protected Range createRange(PsiElement fieldOrMethod) {
		if (fieldOrMethod instanceof PsiField) {
			TextRange tr = ((PsiField) fieldOrMethod).getNameIdentifier().getTextRange();
			return utils.toRange(typeRoot, tr.getStartOffset(), tr.getLength());
		}
		if (fieldOrMethod instanceof PsiLiteralValue) {
			TextRange tr = ((PsiLiteralValue) fieldOrMethod).getTextRange();
			return utils.toRange(typeRoot, tr.getStartOffset(), tr.getLength());
		}
		PsiMethod method = (PsiMethod) fieldOrMethod;
		PsiIdentifier methodName = method.getNameIdentifier();
		TextRange tr = methodName.getTextRange();
		return utils.toRange(typeRoot, tr.getStartOffset(), tr.getLength());
	}

	protected abstract void collectTemplateLink(PsiElement node, PsiLiteralValue locationAnnotation, PsiClass type,
												String className, String fieldOrMethodName, String location, VirtualFile templateFile, String templateFilePath);

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
