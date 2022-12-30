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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.java;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.InsertAnnotationMissingQuickFix;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.MicroProfileRestClientConstants;

/**
 * QuickFix for fixing
 * {@link MicroProfileRestClientErrorCode#InjectAndRestClientAnnotationMissing}
 * error by providing several code actions:
 *
 * <ul>
 * <li>Insert @Inject and @RestClient annotation and the proper import.</li>
 * </ul>
 *
 * @author Angelo ZERR
 *
 */
public class InjectAndRestClientAnnotationMissingQuickFix extends InsertAnnotationMissingQuickFix {

	public InjectAndRestClientAnnotationMissingQuickFix() {
		super(true, MicroProfileConfigConstants.INJECT_ANNOTATION,
				MicroProfileRestClientConstants.REST_CLIENT_ANNOTATION);
	}

	@Override
	public String getParticipantId() {
		return InjectAndRestClientAnnotationMissingQuickFix.class.getName();
	}

}
