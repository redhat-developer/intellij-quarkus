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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.qute.psi.QuteCommandConstants;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Range;

import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.GenerateTemplateInfo;

/**
 * Report codelens for opening/creating Qute template for:
 * 
 * <ul>
 * <li>declared method which have class annotated with @CheckedTemplate.</li>
 * <li>declared field which have Template as type.</li>
 * </ul>
 * 
 * @author Angelo ZERR
 *
 */
public class QuteJavaCodeLensCollector extends AbstractQuteTemplateLinkCollector {

	private static final String QUTE_COMMAND_OPEN_URI_MESSAGE = "Open `{0}`";

	private static final String QUTE_COMMAND_GENERATE_TEMPLATE_MESSAGE = "Create `{0}`";

	private final List<CodeLens> lenses;

	public QuteJavaCodeLensCollector(PsiFile typeRoot, List<CodeLens> lenses, IPsiUtils utils,
									 ProgressIndicator monitor) {
		super(typeRoot, utils, monitor);
		this.lenses = lenses;
	}

	@Override
	protected void collectTemplateLink(PsiElement fieldOrMethod, PsiLiteralValue locationAnnotation, PsiClass type, String className, String fieldOrMethodName,
									   String location, VirtualFile templateFile, String templateFilePath) {
		Command command = null;
		if (templateFile != null) {
			command = new Command(MessageFormat.format(QUTE_COMMAND_OPEN_URI_MESSAGE, templateFilePath), //
					QuteCommandConstants.QUTE_COMMAND_OPEN_URI,
					Arrays.asList(LSPIJUtils.toUri(templateFile).toString()));
		} else {
			List<DataModelParameter> parameters = createParameters(fieldOrMethod);
			GenerateTemplateInfo info = new GenerateTemplateInfo();
			info.setParameters(parameters);
			info.setProjectUri(PsiQuteProjectUtils.getProjectURI(utils.getModule()));
			info.setTemplateFileUri(getVirtualFileUrl(utils.getModule(), templateFilePath, ""));
			info.setTemplateFilePath(templateFilePath);
			command = new Command(MessageFormat.format(QUTE_COMMAND_GENERATE_TEMPLATE_MESSAGE, templateFilePath), //
					QuteCommandConstants.QUTE_COMMAND_GENERATE_TEMPLATE_FILE, Arrays.asList(info));
		}
		TextRange tr = fieldOrMethod.getTextRange();
		Range range = utils.toRange(typeRoot, tr.getStartOffset(), tr.getLength());
		CodeLens codeLens = new CodeLens(range, command, null);
		lenses.add(codeLens);
	}

	private static List<DataModelParameter> createParameters(PsiElement node) {
		if (node instanceof PsiMethod) {
			return createParameter((PsiMethod) node);
		}
		return Collections.emptyList();
	}

	private static List<DataModelParameter> createParameter(PsiField node) {
		List<DataModelParameter> parameters = new ArrayList<>();
		return parameters;
	}

	private static List<DataModelParameter> createParameter(PsiMethod method) {
		List<DataModelParameter> parameters = new ArrayList<>();
		@SuppressWarnings("rawtypes")
		PsiParameter[] methodParameters = method.getParameterList().getParameters();
		for (PsiParameter methodParameter : methodParameters) {
			PsiParameter variable = (PsiParameter) methodParameter;
			String parameterName = variable.getName();
			PsiType parameterType = variable.getType();
			//ITypeBinding binding = parameterType.resolveBinding();
			DataModelParameter parameter = new DataModelParameter();
			parameter.setKey(parameterName);
			parameter.setSourceType(parameterType.getPresentableText());
			parameters.add(parameter);
		}
		return parameters;
	}

}
