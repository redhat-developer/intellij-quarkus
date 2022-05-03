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
package com.redhat.devtools.intellij.quarkus.psi.internal.utils;

import java.util.List;
import java.util.Map;

public class YamlUtils {

	private YamlUtils() {}

	/**
	 * Returns the value extracted from the map as a String, or null if the value is
	 * not in the map
	 *
	 * @param segments the keys to use when searching for the value
	 * @param mapOrValue the map from a property key to another portion of the map, or the
	 * @return the value extracted from the map as a String, or null if the value is
	 *         not in the map
	 */
	public static String getValueRecursively(List<String> segments, Object mapOrValue) {
		if (mapOrValue == null) {
			return null;
		}
		if (segments.size() == 0) {
			return mapOrValue.toString();
		} else if (segments.size() > 0 && mapOrValue instanceof Map<?, ?>) {
			Map<String, Object> configMap = (Map<String, Object>) mapOrValue;
			Object configChild = configMap.get(segments.get(0));
			return getValueRecursively(segments.subList(1, segments.size()), configChild);
		} else {
			return null;
		}
	}
}
