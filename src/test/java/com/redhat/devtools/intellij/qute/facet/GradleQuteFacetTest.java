/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.facet;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.redhat.devtools.intellij.GradleTestCase;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Quarkus facet test
 */
public class GradleQuteFacetTest extends GradleTestCase {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		FileUtils.copyDirectory(new File("projects/gradle/qute"), new File(getProjectPath()));
		importProject();
	}

	@Test
	public void testQuteFacet() throws Exception {
		Module module = getModule("qute.main");
		FacetManager manager = FacetManager.getInstance(module);
		QuteFacet facet = manager.getFacetByType(QuteFacet.FACET_TYPE_ID);
		assertNotNull(facet);
	}
}
