/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.template.resolvedtype;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.redhat.qute.commons.ResolvedJavaTypeInfo;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverKind;

/**
 * API to create {@link ResolvedJavaTypeInfo} from a given IJ {@link com.intellij.psi.PsiClass}.
 * 
 * @author Angelo ZERR
 *
 */
public interface IResolvedJavaTypeFactory {

	/**
	 * Returns true if the factory can deal the given value resolver kind and false
	 * otherwise.
	 * 
	 * @param kind the value resolver kind.
	 * 
	 * @return true if the factory can deal the given value resolver kind and false
	 *         otherwise.
	 */
	boolean isAdaptedFor(ValueResolverKind kind);

	/**
	 * Returns an instance of {@link ResolvedJavaTypeInfo} from the given IJ
	 * {@link com.intellij.psi.PsiClass}.
	 * 
	 * @param type the IJ Java type.
	 * 
	 * @return an instance of {@link ResolvedJavaTypeInfo} from the given JDT
	 *         {@link com.intellij.psi.PsiClass}.
	 *
	 */
	ResolvedJavaTypeInfo create(PsiClass type, Module javaProject);

}