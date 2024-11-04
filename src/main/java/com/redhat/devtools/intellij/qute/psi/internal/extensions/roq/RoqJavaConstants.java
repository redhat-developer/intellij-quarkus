/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.extensions.roq;

import java.util.Collection;
import java.util.List;

/**
 * Roq Java constants.
 *
 * @author Angelo ZERR
 *
 */
public class RoqJavaConstants {

	private RoqJavaConstants() {
	}

	public static final String ROQ_ARTIFACT_ID = "quarkus-roq-frontmatter";

	public static final Collection<String> ROQ_MAVEN_COORS = List.of("io.quarkiverse.roq:quarkus-roq-frontmatter");

	public static final String DATA_MAPPING_ANNOTATION = "io.quarkiverse.roq.data.runtime.annotations.DataMapping";

	public static final String SITE_CLASS = "io.quarkiverse.roq.frontmatter.runtime.model.Site";

	public static final String PAGE_CLASS = "io.quarkiverse.roq.frontmatter.runtime.model.Page";

}