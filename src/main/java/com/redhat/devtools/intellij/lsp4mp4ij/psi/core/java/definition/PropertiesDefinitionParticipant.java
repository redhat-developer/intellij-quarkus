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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.definition;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.MicroProfileConfigPropertyInformation;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.MicroProfileDefinition;

/**
 * Java definition participant to go to the definition of the the property
 * declared in a member value of annotation.
 * 
 * For instance:
 * 
 * <ul>
 * <li>from Java annotation: &#64;ConfigProperty(name="foo.bar")</li>
 * <li>to properties file : foo.bar = 10</li>
 * </ul>
 * 
 * @author Angelo ZERR
 *
 */
public class PropertiesDefinitionParticipant extends AbstractAnnotationDefinitionParticipant {

	public PropertiesDefinitionParticipant(String annotationName, String[] annotationAttributeNames) {
		super(annotationName, annotationAttributeNames);
	}

	public PropertiesDefinitionParticipant(String annotationName, String[] annotationAttributeNames,
										   Function<String, String> propertyReplacer) {
		super(annotationName, annotationAttributeNames, propertyReplacer);
	}

	@Override
	protected List<MicroProfileDefinition> collectDefinitions(String propertyKey, Range propertyKeyRange,
															  PsiAnnotation annotation, JavaDefinitionContext context) {
		Module javaProject = context.getJavaProject();
		// Collect all properties files (properties, yaml files) where the given
		// property key is configured
		List<MicroProfileConfigPropertyInformation> infos = PsiMicroProfileProjectManager.getInstance(context.getJavaProject().getProject())
				.getMicroProfileProject(javaProject).getPropertyInformations(propertyKey);
		if (!infos.isEmpty()) {
			return infos.stream().map(info -> {
				MicroProfileDefinition definition = new MicroProfileDefinition();
				definition.setSelectPropertyName(info.getPropertyNameWithProfile());
				LocationLink location = new LocationLink();
				definition.setLocation(location);
				location.setTargetUri(info.getSourceConfigFileURI());
				location.setOriginSelectionRange(propertyKeyRange);
				return definition;
			}).collect(Collectors.toList());
		}
		return null;
	}

}
