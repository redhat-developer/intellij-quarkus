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
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

/**
 * Quarkus library test
 */
public class MavenQuteFacetTest extends MavenModuleImportingTestCase {

	public void testDummy() {}
	
	@Test
	@Ignore("Task are missing for Qute")
	public void QuteFacet() throws Exception {
		Module module = createMavenModule("qute", new File("projects/maven/qute"));
		FacetManager manager = FacetManager.getInstance(module);
		QuteFacet facet = manager.getFacetByType(QuteFacet.FACET_TYPE_ID);
		assertNotNull(facet);
	}

}
