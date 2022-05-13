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
package com.redhat.microprofile.psi.internal.quarkus.scheduler.java;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.hover.PropertiesHoverParticipant;
import com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants;

import java.util.function.Function;

import static com.redhat.devtools.intellij.lsp4mp4ij.commons.PropertyReplacerStrategy.EXPRESSION_REPLACER;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.SCHEDULED_SUPPORTED_PARTICIPANT_MEMBERS;

public class QuarkusScheduledHoverParticipant extends PropertiesHoverParticipant {

	public QuarkusScheduledHoverParticipant() {
		super(QuarkusConstants.SCHEDULED_ANNOTATION, SCHEDULED_SUPPORTED_PARTICIPANT_MEMBERS, null,
				EXPRESSION_REPLACER);
	}

	@Override
	protected boolean isAdaptableFor(PsiElement hoverElement) {
		return hoverElement instanceof PsiMethod;
	}
}
