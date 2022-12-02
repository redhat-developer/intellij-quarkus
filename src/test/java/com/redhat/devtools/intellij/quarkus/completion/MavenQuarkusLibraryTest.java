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

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import org.junit.Test;

import java.io.File;
import java.util.stream.Stream;

/**
 * Quarkus library test
 */
public class MavenQuarkusLibraryTest extends MavenModuleImportingTestCase {

	@Test
	public void testQuarkusLibraryDoesNotReplicateExistingDependencies() throws Exception {
		Module module = createMavenModule(new File("projects/maven/config-quickstart"));
		ModifiableRootModel model = ReadAction.compute(() -> ModuleRootManager.getInstance(module).getModifiableModel());
		LibraryTable table = model.getModuleLibraryTable();
		Library library = table.getLibraryByName(QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_NAME);
		assertNotNull(library);
		assertFalse(Stream.of(library.getFiles(OrderRootType.CLASSES)).filter(f -> f.getName().contains("commons-io")).findAny().isPresent());
	}

}
