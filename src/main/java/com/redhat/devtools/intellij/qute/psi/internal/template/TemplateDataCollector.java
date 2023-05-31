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
package com.redhat.devtools.intellij.qute.psi.internal.template;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiType;

import com.redhat.qute.commons.datamodel.DataModelBaseTemplate;
import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelTemplate;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.JAVA_LANG_OBJECT_TYPE;
/**
 * AST visitor used to collect {@link DataModelParameter} parameter for a given
 * {@link DataModelTemplate} template.
 * 
 * This visitor track the invocation of method
 * io.quarkus.qute.Template#data(String key, Object data) to collect parameters.
 * 
 * For instance, with this following code:
 * 
 * <code>
 * private final Template page;
 * ...
 * page.data("age", 13);
   page.data("name", "John");
 * </code>
 * 
 * the AST visitor will collect the following parameters:
 * 
 * <ul>
 * <li>parameter key='age', sourceType='int'</li>
 * <li>parameter key='name', sourceType='java.lang.String'</li>
 * </ul>
 * 
 * @author Angelo ZERR
 *
 */
public class TemplateDataCollector extends TemplateDataVisitor {

	private final DataModelBaseTemplate<DataModelParameter> template;

	public TemplateDataCollector(DataModelBaseTemplate<DataModelParameter> template, ProgressIndicator monitor) {
		this.template = template;
	}

	@Override
	protected boolean visitParameter(Object name, Object type) {
		String paramName = null;
		if (name instanceof PsiLiteral) {
			paramName = ((PsiLiteral) name).getValue().toString();
		}
		if (paramName != null) {
			String paramType = JAVA_LANG_OBJECT_TYPE;
			if (type instanceof PsiExpression) {
				PsiType binding = ((PsiExpression) type).getType();
				paramType = binding.getCanonicalText();
			}

			if (paramName != null && template.getParameter(paramName) == null) {
				DataModelParameter parameter = new DataModelParameter();
				parameter.setKey(paramName);
				parameter.setSourceType(paramType);
				parameter.setDataMethodInvocation(true);
				template.addParameter(parameter);
			}
		}
		return true;
	}
}
