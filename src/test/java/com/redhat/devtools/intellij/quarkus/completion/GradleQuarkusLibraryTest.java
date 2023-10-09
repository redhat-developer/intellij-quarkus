/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.completion;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.redhat.devtools.intellij.GradleTestCase;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import com.redhat.devtools.intellij.quarkus.QuarkusDeploymentSupport;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Quarkus library test
 */
public class GradleQuarkusLibraryTest extends GradleTestCase {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		FileUtils.copyDirectory(new File("projects/gradle/config-quickstart"), new File(getProjectPath()));
		importProject();
	}

	@Test
	public void testQuarkusLibraryDoesNotReplicateExistingDependencies() throws Exception {
		Module module = getModule("config-quickstart.native-test");
		QuarkusDeploymentSupport.getInstance(module.getProject()).updateClasspathWithQuarkusDeployment(module, new EmptyProgressIndicator());

		Optional<LibraryOrderEntry> library = Stream.of(ModuleRootManager.getInstance(module).getOrderEntries()).filter(entry -> entry instanceof LibraryOrderEntry).
				map(entry -> LibraryOrderEntry.class.cast(entry)).filter(entry -> entry.getLibraryName().equals(QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_NAME)).findFirst();
		assertTrue(library.isPresent());
		assertNotNull(library.get().getLibrary());
		assertFalse(Stream.of(library.get().getLibrary().getFiles(OrderRootType.CLASSES)).filter(f -> f.getName().contains("commons-io")).findAny().isPresent());
	}
}
