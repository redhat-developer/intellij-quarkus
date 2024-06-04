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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiLiteral;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;

import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelTemplate;

import java.util.concurrent.CancellationException;

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
public class TemplateDataLocation extends TemplateDataVisitor {

	private final String parameterName;

	private final IPsiUtils utils;

	private Location location;

	public TemplateDataLocation(String parameterName, IPsiUtils utils) {
		this.parameterName = parameterName;
		this.utils = utils;
	}

	@Override
	protected boolean visitParameter(Object paramName, Object paramType) {
		if (paramName instanceof PsiLiteral) {
			PsiLiteral literal = ((PsiLiteral) paramName);
			String paramNameString = literal.getValue().toString();
			if (parameterName.equals(paramNameString)) {
				try {
					Range range = utils.toRange(getMethod(), literal.getTextOffset(),
							literal.getTextLength());
					String uri = utils.toUri(getMethod().getContainingFile());
					this.location = new Location(uri, range);
				} catch (ProcessCanceledException e) {
					//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
					//TODO delete block when minimum required version is 2024.2
					throw e;
				} catch (IndexNotReadyException | CancellationException e) {
					throw e;
				} catch (RuntimeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return false;
			}
		}
		return true;
	}

	public Location getLocation() {
		return location;
	}
}
