/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.template;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants;
import com.redhat.devtools.intellij.qute.psi.utils.TextEditConverter;
import com.redhat.qute.commons.GenerateMissingJavaMemberParams;
import org.eclipse.lsp4j.CreateFile;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Resolves workspace edits for generating missing java members.
 *
 * @author datho7561
 */
public class QuteSupportForTemplateGenerateMissingJavaMemberHandler {

	private static final Logger LOGGER = Logger
			.getLogger(QuteSupportForTemplateGenerateMissingJavaMemberHandler.class.getName());

	private static final Pattern BROKEN_FILE_PROTOCOL = Pattern.compile("file:/(?!//)");

	private static final Range PREPEND_RANGE = new Range(new Position(0, 0), new Position(0, 0));

	private QuteSupportForTemplateGenerateMissingJavaMemberHandler() {

	}

	/**
	 * Returns the WorkspaceEdit needed to generate the requested member, or null if
	 * it cannot be computed.
	 *
	 * @param params  the parameters needed to construct the workspace edit
	 * @param utils   the jdt utils
	 * @param monitor the progress monitor
	 * @return the WorkspaceEdit needed to generate the requested member, or null if
	 *         it cannot be computed
	 */
	public static WorkspaceEdit handleGenerateMissingJavaMember(GenerateMissingJavaMemberParams params, IPsiUtils utils,
			ProgressIndicator monitor) {
		switch (params.getMemberType()) {
		case Field:
			return handleMissingField(params, utils, monitor);
		case Getter:
			return handleCreateMissingGetterCodeAction(params, utils, monitor);
		case AppendTemplateExtension:
			return handleCreateMissingTemplateExtension(params, utils, monitor);
		case CreateTemplateExtension:
			return createNewTemplateExtensionsFile(params, utils, monitor);
		default:
			return null;
		}
	}

	private static WorkspaceEdit handleMissingField(GenerateMissingJavaMemberParams params, IPsiUtils utils,
			ProgressIndicator monitor) {
		Module project = getJavaProjectFromProjectUri(params.getProjectUri(), utils);
		PsiClass javaType;
		try {
			javaType = utils.findClass(project, params.getJavaType());
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			return null;
		}

		PsiField currentlyField = javaType.findFieldByName(params.getMissingProperty(), false);
		if (currentlyField != null && currentlyField.isValid()) {
			return handleUpdatePermissionsOfExistingField(params, utils, project, javaType, monitor);
		} else {
			return handleCreateMissingField(params, utils, project, javaType, monitor);
		}

	}

	private static WorkspaceEdit handleCreateMissingField(GenerateMissingJavaMemberParams params, IPsiUtils utils,
			Module project, PsiClass javaType, ProgressIndicator monitor) {
		PsiFile cu = createQuickFixAST(javaType);
		if (cu == null) {
			return null;
		}
		PsiClass newTypeDecl = PsiTreeUtil.findSameElementInCopy(javaType, cu);
		if (newTypeDecl == null) {
			return null;
		}
		PsiField field = JavaPsiFacade.getElementFactory(project.getProject()).createField(params.getMissingProperty(),
				PsiType.getJavaLangString(javaType.getManager(), GlobalSearchScope.moduleWithLibrariesScope(project)));
		field.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
		newTypeDecl.addAfter(field, newTypeDecl.getFields().length > 0 ?
				newTypeDecl.getFields()[newTypeDecl.getFields().length - 1] : null);
		CodeStyleManager.getInstance(newTypeDecl.getProject()).reformat(newTypeDecl);
		Document jdtTextEdit;
		try {
			jdtTextEdit = cu.getViewProvider().getDocument();
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			return null;
		}
		TextDocumentEdit textDocumentEdit = new TextEditConverter(javaType.getContainingFile(), cu,
				jdtTextEdit, utils).convertToTextDocumentEdit(0);
		return new WorkspaceEdit(Arrays.asList(Either.forLeft(textDocumentEdit)));
	}

	private static WorkspaceEdit handleUpdatePermissionsOfExistingField(GenerateMissingJavaMemberParams params,
			IPsiUtils utils, Module project, PsiClass javaType, ProgressIndicator monitor) {
		PsiFile cu = createQuickFixAST(javaType);
		if (cu == null) {
			return null;
		}
		PsiClass newTypeDecl = PsiTreeUtil.findSameElementInCopy(javaType, cu);
		if (newTypeDecl == null) {
			return null;
		}
		PsiField oldFieldDeclaration = newTypeDecl.findFieldByName(params.getMissingProperty(), false);
		if (oldFieldDeclaration == null) {
			return null;
		}

		// This is needed to prevent an exception, since the language client might send
		// a new CodeAction request before the user saves the modifications to the Java
		// class from accepting the previous CodeAction
		boolean alreadyPublic = oldFieldDeclaration.getModifierList().hasExplicitModifier(PsiModifier.PUBLIC);
		if (alreadyPublic) {
			return null;
		}
		oldFieldDeclaration.normalizeDeclaration();
		oldFieldDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
		CodeStyleManager.getInstance(newTypeDecl.getProject()).reformat(newTypeDecl);

		Document jdtTextEdit;
		try {
			jdtTextEdit = cu.getViewProvider().getDocument();
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			return null;
		}
		TextDocumentEdit textDocumentEdit = new TextEditConverter(javaType.getContainingFile(), cu,
				jdtTextEdit, utils).convertToTextDocumentEdit(0);
		return new WorkspaceEdit(Arrays.asList(Either.forLeft(textDocumentEdit)));
	}

	private static WorkspaceEdit handleCreateMissingGetterCodeAction(GenerateMissingJavaMemberParams params,
			IPsiUtils utils, ProgressIndicator monitor) {
		Module project = getJavaProjectFromProjectUri(params.getProjectUri(), utils);
		PsiClass javaType;
		try {
			javaType = utils.findClass(project, params.getJavaType());
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			return null;
		}
		PsiFile cu = createQuickFixAST(javaType);
		if (cu == null) {
			return null;
		}
		PsiClass newTypeDecl = PsiTreeUtil.findSameElementInCopy(javaType, cu);
		if (newTypeDecl == null) {
			return null;
		}

		var factory = JavaPsiFacade.getElementFactory(project.getProject());
		PsiField field = javaType.findFieldByName(params.getMissingProperty(), false);
		String methodName = "get" + getCapitalized(params.getMissingProperty());
		PsiMethod methodDeclaration = factory.createMethod(methodName, field != null ?
				field.getType() : PsiType.getJavaLangString(javaType.getManager(), GlobalSearchScope.moduleWithLibrariesScope(project)));
		methodDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);

		if (field != null && field.isValid()) {
			var code = factory.createCodeBlockFromText("{\nreturn " + params.getMissingProperty() + ";\n}",
					null);
			methodDeclaration.getBody().replace(code);
		} else {
			var code = factory.createCodeBlockFromText("{\nreturn null;\n}", null);
			methodDeclaration.getBody().replace(code);
		}
		newTypeDecl.add(methodDeclaration);
		CodeStyleManager.getInstance(project.getProject()).reformat(newTypeDecl);
		Document jdtTextEdit;
		try {
			jdtTextEdit = cu.getViewProvider().getDocument();
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			return null;
		}
		TextDocumentEdit textDocumentEdit = new TextEditConverter(javaType.getContainingFile(), cu,
				jdtTextEdit, utils).convertToTextDocumentEdit(0);
		return new WorkspaceEdit(Arrays.asList(Either.forLeft(textDocumentEdit)));
	}

	// TODO: caching scheme
	private static WorkspaceEdit handleCreateMissingTemplateExtension(GenerateMissingJavaMemberParams params,
			IPsiUtils utils, ProgressIndicator monitor) {

		Module project = getJavaProjectFromProjectUri(params.getProjectUri(), utils);
		PsiClass type = null;
		try {
			type = utils.findClass(project, params.getTemplateClass());
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING,
					String.format("JavaModelException while trying to locate template extension class {0}",
							params.getTemplateClass()),
					e);
		}

		if (type == null) {
			return null;
		}
		return addTemplateExtensionToFile(params, utils, project, type, monitor);
	}

	private static WorkspaceEdit createNewTemplateExtensionsFile(GenerateMissingJavaMemberParams params,
			IPsiUtils utils, ProgressIndicator monitor) {
		Module project = getJavaProjectFromProjectUri(params.getProjectUri(), utils);
		return createNewTemplateExtensionFile(params, utils, project, monitor);
	}

	private static WorkspaceEdit addTemplateExtensionToFile(GenerateMissingJavaMemberParams params, IPsiUtils utils,
			Module project, PsiClass templateExtensionType, ProgressIndicator monitor) {
		PsiFile cu = createQuickFixAST(templateExtensionType);
		PsiClass newTypeDecl = PsiTreeUtil.findSameElementInCopy(templateExtensionType, cu);
		if (newTypeDecl == null) {
			return null;
		}

		var factory = JavaPsiFacade.getElementFactory(project.getProject());
		String methodName = params.getMissingProperty();
		var returnType = PsiType.getJavaLangString(templateExtensionType.getManager(),
				GlobalSearchScope.moduleWithLibrariesScope(project));
		PsiMethod methodDeclaration = factory.createMethod(methodName, returnType);
		methodDeclaration.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
		methodDeclaration.getModifierList().setModifierProperty(PsiModifier.STATIC, true);
		var parameter = factory.createParameter(getParamNameFromFullyQualifiedType(params.getJavaType()),
				PsiType.getTypeByName(params.getJavaType(), project.getProject(),
						GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(project)));
		methodDeclaration.getParameterList().add(parameter);
		var code = factory.createCodeBlockFromText("{\nreturn null;\n}", null);
		methodDeclaration.getBody().replace(code);
		newTypeDecl.add(methodDeclaration);
		CodeStyleManager.getInstance(newTypeDecl.getProject()).reformat(newTypeDecl);

		Document jdtTextEdit;
		try {
			jdtTextEdit = cu.getViewProvider().getDocument();
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			return null;
		}
		TextDocumentEdit textDocumentEdit = new TextEditConverter(
				templateExtensionType.getContainingFile(), cu, jdtTextEdit, utils)
				.convertToTextDocumentEdit(0);
		return new WorkspaceEdit(Arrays.asList(Either.forLeft(textDocumentEdit)));
	}

	private static WorkspaceEdit createNewTemplateExtensionFile(GenerateMissingJavaMemberParams params, IPsiUtils utils,
			Module project, ProgressIndicator monitor) {
		var sources = ModuleRootManager.getInstance(project).getSourceRoots(JavaSourceRootType.SOURCE);
		var destPackage = sources != null && !sources.isEmpty() ? sources.get(0) : null;
		// TODO: just use the default package I guess?
		if (destPackage == null) {
			return null;
		}

		String baseName = "TemplateExtensions";
		String name = baseName;
		VirtualFile cu = destPackage.findChild(baseName + ".java");
		int i = 0;
		while (cu != null && cu.isValid()) {
			name = baseName + i++;
			cu = destPackage.findChild(name + ".java");
		}

		ResourceOperation createFileOperation = new CreateFile(
				fixBrokenUri(destPackage.getUrl() + "/" + name + ".java"));
		TextDocumentEdit addContentEdit;
		try {
			addContentEdit = createNewTemplateExtensionsContent(cu, name, params.getMissingProperty(),
					params.getJavaType(), fixBrokenUri(destPackage.getUrl() + "/" + name + ".java"), utils);
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failure while constructing new Java file content", e);
		}

		WorkspaceEdit makeTemplateExtensions = new WorkspaceEdit();
		makeTemplateExtensions.setDocumentChanges(
				Arrays.asList(Either.forRight(createFileOperation), Either.forLeft(addContentEdit)));
		return makeTemplateExtensions;
	}

	private static TextDocumentEdit createNewTemplateExtensionsContent(VirtualFile cu, String typeName,
			String methodName, String methodParamFullyQualifiedType, String uri, IPsiUtils utils) {
		String lineDelimiter = CodeStyleSettingsManager.getInstance(utils.getProject()).getMainProjectCodeStyle().
				getLineSeparator();
		if (lineDelimiter == null) {
			lineDelimiter = System.lineSeparator();
		}

		String typeStub = constructTypeStub(cu, typeName, PsiModifier.PUBLIC, methodName, methodParamFullyQualifiedType,
				lineDelimiter);
		String cuContent = constructCUContent(cu, typeStub, lineDelimiter);
		TextDocumentEdit tde = new TextDocumentEdit();
		tde.setTextDocument(new VersionedTextDocumentIdentifier(uri, 0));
		tde.setEdits(Arrays.asList(new TextEdit(PREPEND_RANGE, cuContent)));
		return tde;
	}

	/*
	 * Copied & modified from JDT-LS
	 */
	private static String constructTypeStub(VirtualFile parentCU, String name, String modifiers, String methodName,
			String methodParamFullyQualifiedType, String lineDelimiter) {
		StringBuilder buf = new StringBuilder();

		buf.append("@");
		buf.append(QuteJavaConstants.TEMPLATE_EXTENSION_ANNOTATION);
		buf.append(lineDelimiter);

		buf.append(modifiers);
		if (!modifiers.isEmpty()) {
			buf.append(' ');
		}

		buf.append("class ");
		buf.append(name);
		buf.append(" {").append(lineDelimiter); //$NON-NLS-1$
		buf.append(constructMethodStub(parentCU, methodName, methodParamFullyQualifiedType, lineDelimiter));
		buf.append('}').append(lineDelimiter);
		return buf.toString();
	}

	private static String constructMethodStub(VirtualFile compilationUnit, String methodName,
			String methodParamFullyQualifiedType, String lineDelimiter) {
		StringBuilder buf = new StringBuilder("\tpublic static String ");
		buf.append(methodName).append("(").append(methodParamFullyQualifiedType);
		buf.append(" ");
		buf.append(getParamNameFromFullyQualifiedType(methodParamFullyQualifiedType));
		buf.append(") {").append(lineDelimiter);
		buf.append("\t\treturn null;").append(lineDelimiter);
		buf.append("\t}").append(lineDelimiter);
		return buf.toString();
	}

	/**
	 * Copied from JDT-LS
	 */
	private static String constructCUContent(VirtualFile cu, String typeContent, String lineDelimiter) {
		return typeContent;
	}

	private static PsiFile createQuickFixAST(PsiClass javaType) {
		if (javaType instanceof PsiCompiledElement) {
			return null;
		}
		return javaType.getContainingFile().getViewProvider().clone().getPsi(javaType.getLanguage());
	}

	private static Module getJavaProjectFromProjectUri(String projectName, IPsiUtils utils) {
		if (projectName == null) {
			return null;
		}
		return ModuleManager.getInstance(utils.getProject()).findModuleByName(projectName);
	}

	/**
	 * Returns the given camelCaseName with the first letter capitalized.
	 *
	 * @param camelCaseName the camelCaseVariableName to capitalize
	 * @return the given camelCaseName with the first letter capitalized
	 */
	private static String getCapitalized(String camelCaseName) {
		return camelCaseName.substring(0, 1).toUpperCase() + camelCaseName.substring(1);
	}

	private static String getParamNameFromFullyQualifiedType(String fullyQualifiedType) {
		int lastDot = fullyQualifiedType.lastIndexOf(".");
		return fullyQualifiedType.substring(lastDot + 1, lastDot + 2).toLowerCase()
				+ fullyQualifiedType.substring(lastDot + 2);
	}

	private static String fixBrokenUri(String uri) {
		/*Matcher m = BROKEN_FILE_PROTOCOL.matcher(uri);
		return m.replaceFirst("file:///");*/
		return VfsUtil.toUri(uri).toString();
	}

}
