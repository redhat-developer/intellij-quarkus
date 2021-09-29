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
package com.redhat.microprofile.psi.internal.quarkus.providers;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.hover.PropertiesHoverParticipant;
import com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants;

public class QuarkusScheduledHoverParticipant extends PropertiesHoverParticipant {

	public QuarkusScheduledHoverParticipant() {
		super(QuarkusConstants.SCHEDULED_ANNOTATION,
				new String [] { "cron", "every", "delay", "delayed", "delayUnit"}, null);
	}

	@Override
	protected boolean isAdaptableFor(PsiElement hoverElement) {
		return hoverElement instanceof PsiMethod;
	}
}
