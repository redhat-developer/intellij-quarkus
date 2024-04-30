/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.scheduler.properties;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameValuePair;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractAnnotationTypeReferencePropertiesProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.IPropertiesCollector;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;
import com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getSourceMethod;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getSourceType;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.isBinary;

/**
 * Properties provider to collect Quarkus properties from Java methods
 * annotated with the "io.quarkus.scheduler.Scheduled".
 * Valid property values will be surrounded with curly braces.
 */
public class QuarkusScheduledPropertiesProvider extends AbstractAnnotationTypeReferencePropertiesProvider {

	private static final String[] ANNOTATION_NAMES = { QuarkusConstants.SCHEDULED_ANNOTATION };

	private static final Pattern PROP_PATTERN = Pattern.compile("\\{(.*)\\}");

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(PsiModifierListOwner javaElement, PsiAnnotation configPropertyAnnotation,
									 String annotationName, SearchContext context) {
		if (javaElement instanceof PsiMethod) {
			String extensionName = null;
			IPropertiesCollector collector = context.getCollector();
			String description = null;
			String sourceMethod = getSourceMethod((PsiMethod) javaElement);
			String sourceType = getSourceType(javaElement);
			boolean binary = isBinary(javaElement);

			for (PsiNameValuePair mvp : configPropertyAnnotation.getParameterList().getAttributes()) {
				String name = mvp.getLiteralValue();
				if (name != null && !name.isEmpty()) {
					Matcher m = PROP_PATTERN.matcher(name);
					if (m.matches()) {
						name = m.group(1);
						int colonIdx = name.indexOf(":");
						if (colonIdx > -1) {//remove default value
							name = name.substring(0,colonIdx);
						}
						addItemMetadata(collector, name, "java.lang.String", description, sourceType, null, sourceMethod, null,
								extensionName, binary);
					}
				}
			}
		}
	}

}
