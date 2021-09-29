/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.quarkus;


import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.MavenImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertPropertiesDuplicate;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.p;


/**
 * Test the availability of the Quarkus Hibernate ORM properties
 * 
 * @author Angelo ZERR
 *
 */
public class MavenQuarkusHibernateORMPropertyTest extends MavenImportingTestCase {

	@Test
	public void testQuarkusContainerImages() throws Exception {

		Module module = createMavenModule("hibernate-orm-resteasy", new File("projects/quarkus/maven/hibernate-orm-resteasy"));
		MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(), DocumentFormat.PlainText);

		assertProperties(infoFromClasspath,

				p("quarkus-hibernate-orm", "quarkus.hibernate-orm.database.generation", "java.lang.String",
						"Select whether the database schema is generated or not."
						+ "\n\n`drop-and-create` is awesome in development mode."
						+ "\n\nAccepted values: `none`, `create`, `drop-and-create`, `drop`, `update`.", true,
						"io.quarkus.hibernate.orm.deployment.HibernateOrmConfig$HibernateOrmConfigDatabase", "generation", null, 1,
						"none"));

		assertPropertiesDuplicate(infoFromClasspath);
	}

}