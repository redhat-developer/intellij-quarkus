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
package com.redhat.devtools.intellij.qute.psi.internal.resolver;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;

/**
 * Class file type resolver implementation.
 * 
 * @author Angelo ZERR
 *
 */
public class ClassFileTypeResolver extends AbstractTypeResolver {

	public ClassFileTypeResolver(PsiClass classFile, Module javaProject) {
		super(classFile, javaProject);
	}

	@Override
	protected String resolveSimpleType(String typeSignature) {
		return typeSignature;
	}

}
