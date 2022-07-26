/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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
package com.redhat.microprofile.psi.internal.quarkus.scheduler.java;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.definition.PropertiesDefinitionParticipant;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.PropertyReplacerStrategy.EXPRESSION_REPLACER;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.SCHEDULED_ANNOTATION;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.SCHEDULED_SUPPORTED_PARTICIPANT_MEMBERS;

/**
 * Quarkus applocation.properties Definition to navigate from Java
 * file @Scheduled/cron, every to properties, yaml files where the property is
 * declared.
 *
 */
public class QuarkusScheduledDefinitionParticipant extends PropertiesDefinitionParticipant {

	public QuarkusScheduledDefinitionParticipant() {
		super(SCHEDULED_ANNOTATION, SCHEDULED_SUPPORTED_PARTICIPANT_MEMBERS, EXPRESSION_REPLACER);
	}
}