/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.gradle;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import com.redhat.devtools.intellij.quarkus.search.core.PropertiesManagerForJava;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4mp.commons.JavaFileInfo;
import org.eclipse.lsp4mp.commons.MicroProfileJavaFileInfoParams;
import org.junit.Test;

import java.io.File;

/**
 * JDT Quarkus manager test for hover in Java file.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java</a>
 *
 */
public class GradlePropertiesManagerForJavaTest extends GradleTestCase {
	private Module javaProject;

	private String getJavaFileUri(String path) {
		return new File(ModuleUtilCore.getModuleDirPath(javaProject), path).toURI().toString();
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		FileUtils.copyDirectory(new File("projects/gradle/config-hover"), new File(getProjectPath()));
		importProject();
		javaProject = getModule("config-hover.main");
	}

	@Test
	public void testFileInfoWithPackage() throws Exception {
		MicroProfileJavaFileInfoParams params = new MicroProfileJavaFileInfoParams();
		String javaFileUri = getJavaFileUri("src/main/java/org/acme/config/GreetingResource.java");
		params.setUri(javaFileUri);
		JavaFileInfo javaFileInfo = PropertiesManagerForJava.getInstance().fileInfo(params, PsiUtilsImpl.getInstance());
		assertNotNull(javaFileInfo);
		assertEquals("org.acme.config", javaFileInfo.getPackageName());
	}

	@Test
	public void testFileInfoWithoutPackage() throws Exception {
		String javaFileUri = getJavaFileUri("src/main/java/NoPackage.java");

		MicroProfileJavaFileInfoParams params = new MicroProfileJavaFileInfoParams();
		params.setUri(javaFileUri);
		JavaFileInfo javaFileInfo = PropertiesManagerForJava.getInstance().fileInfo(params, PsiUtilsImpl.getInstance());
		assertNotNull(javaFileInfo);
		assertEquals("", javaFileInfo.getPackageName());
	}

	@Test
	public void testFileInfoNull() throws Exception {
		String javaFileUri = getJavaFileUri("src/main/java/BAD_JAVA_FILE.java");

		MicroProfileJavaFileInfoParams params = new MicroProfileJavaFileInfoParams();
		params.setUri(javaFileUri);
		JavaFileInfo javaFileInfo = PropertiesManagerForJava.getInstance().fileInfo(params, PsiUtilsImpl.getInstance());
		assertNull(javaFileInfo);
	}
}
