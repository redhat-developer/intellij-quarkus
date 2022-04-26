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
package com.redhat.devtools.intellij.qute.psi.internal.java;

import java.text.MessageFormat;

/**
 * Qute error code API.
 *
 */
public interface IQuteErrorCode {

	/**
	 * Returns the XML error code.
	 * 
	 * @return the Qute error code.
	 */
	String getCode();

	/**
	 * Returns the raw message.
	 * 
	 * @return the raw message.
	 */
	String getRawMessage();

	default String getMessage(Object... arguments) {
		return MessageFormat.format(getRawMessage(), arguments);
	}
}
