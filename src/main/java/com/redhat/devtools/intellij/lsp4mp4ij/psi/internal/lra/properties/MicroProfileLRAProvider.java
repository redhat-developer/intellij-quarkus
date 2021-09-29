/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.lra.properties;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractStaticPropertiesProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.lra.MicroProfileLRAConstants.LRA_ANNOTATION;

/**
 * Properties provider that provides static MicroProfile LRA properties
 * 
 * @author David Kwon
 * 
 * @see <a href="https://github.com/eclipse/microprofile-lra/blob/2d7b24b4bcb755eadb19c74dadd504cc41b0c094/spec/src/main/asciidoc/microprofile-lra-spec.adoc#322-configuration-parameters"></a>https://github.com/eclipse/microprofile-lra/blob/2d7b24b4bcb755eadb19c74dadd504cc41b0c094/spec/src/main/asciidoc/microprofile-lra-spec.adoc#322-configuration-parameters</a>
 *
 */
public class MicroProfileLRAProvider extends AbstractStaticPropertiesProvider {
	
	public MicroProfileLRAProvider() {
		super("/static-properties/mp-lra-metadata.json");
	}

	@Override
	protected boolean isAdaptedFor(SearchContext context) {
		Module javaProject = context.getJavaProject();
		return (PsiTypeUtils.findType(javaProject, LRA_ANNOTATION) != null);
	}
}
