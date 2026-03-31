/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.extensions.flags;

import java.util.Collection;
import java.util.List;

/**
 * Flags Java constants.
 * 
 * @author Angelo ZERR
 *
 */
public class FlagsJavaConstants {

	private FlagsJavaConstants() {}

	public static final String FLAGS_ARTIFACT_ID = "quarkus-flags-qute";

	public static final Collection<String> FLAGS_MAVEN_COORS = List.of("io.quarkiverse.flags:quarkus-flags-qute");

}
