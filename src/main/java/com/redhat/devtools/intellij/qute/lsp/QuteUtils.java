/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.lsp;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Red Hat Developers
 *
 */
public class QuteUtils {

	public static Map<String, Object> getQuteSettings() {
		Map<String, Object> settings = new HashMap<>();
		Map<String, Object> qute = new HashMap<>();
		Map<String, Object> workspaceFolders = new HashMap<>();
		qute.put("workspaceFolders", workspaceFolders);
		settings.put("qute", qute);
		return settings;
	}
}
