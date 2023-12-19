/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.template;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.qute.psi.QuteMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.qute.psi.QuteMavenProjectName;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.qute.commons.DocumentFormat;
import com.redhat.qute.commons.QuteJavadocParams;
import org.junit.Test;

import java.io.File;

/**
 * Tests for getting the formatted Javadocs for Java members
 * 
 * @author datho7561
 */
public class TemplateGetJavadocTest extends QuteMavenModuleImportingTestCase {

	@Test
	public void testgetFieldJavadoc() throws Exception {
		loadMavenProject(QuteMavenProjectName.qute_quickstart);

		QuteJavadocParams params = new QuteJavadocParams(//
				"org.acme.qute.Item", //
				QuteMavenProjectName.qute_quickstart, //
				"name", //
				"name : java.lang.String", //
				DocumentFormat.Markdown);

		String actual = QuteSupportForTemplate.getInstance().getJavadoc(params, getJDTUtils(), new EmptyProgressIndicator());
		String expected = "The name of the item";
		assertEquals(expected, actual);
	}
	
	@Test
	public void testgetMethodJavadoc() throws Exception {
		loadMavenProject(QuteMavenProjectName.qute_quickstart);

		QuteJavadocParams params = new QuteJavadocParams(//
				"org.acme.qute.Item", //
				QuteMavenProjectName.qute_quickstart, //
				"getDerivedItems", //
				"getDerivedItems() : org.acme.qute.Item[]", //
				DocumentFormat.Markdown);

		String actual = QuteSupportForTemplate.getInstance().getJavadoc(params, getJDTUtils(), new EmptyProgressIndicator());
		String expected = """
				Returns the derived items.
				
				* **Returns:**
				  * the derived items""";
		assertEquals(expected, actual);
	}
	
	@Test
	public void testgetFieldJavadocPlainText() throws Exception {
		loadMavenProject(QuteMavenProjectName.qute_quickstart);

		QuteJavadocParams params = new QuteJavadocParams(//
				"org.acme.qute.Item", //
				QuteMavenProjectName.qute_quickstart, //
				"name", //
				"name : java.lang.String", //
				DocumentFormat.PlainText);

		String actual = QuteSupportForTemplate.getInstance().getJavadoc(params, getJDTUtils(), new EmptyProgressIndicator());
		String expected = " The name of the item ";
		assertEquals(expected, actual);
	}
	
	@Test
	public void testgetMethodJavadocPlainText() throws Exception {
		loadMavenProject(QuteMavenProjectName.qute_quickstart);

		QuteJavadocParams params = new QuteJavadocParams(//
				"org.acme.qute.Item", //
				QuteMavenProjectName.qute_quickstart, //
				"getDerivedItems", //
				"getDerivedItems() : org.acme.qute.Item[]", //
				DocumentFormat.PlainText);

		String actual = QuteSupportForTemplate.getInstance().getJavadoc(params, getJDTUtils(), new EmptyProgressIndicator());
		String expected = " Returns the derived items. \n * Returns:\n   - the derived items";
		assertEquals(expected, actual);
	}
	
	@Test
	public void testgetMethodJavadocCyclic() throws Exception {
		loadMavenProject(QuteMavenProjectName.qute_quickstart);

		QuteJavadocParams params = new QuteJavadocParams(//
				"org.acme.qute.cyclic.ClassC", //
				QuteMavenProjectName.qute_quickstart, //
				"convert", //
				"convert() : java.lang.String", //
				DocumentFormat.PlainText);

		String actual = QuteSupportForTemplate.getInstance().getJavadoc(params, getJDTUtils(), new EmptyProgressIndicator());
		String expected = " cyclic documentation ";
		assertEquals(expected, actual);
	}
	
	@Test
	public void testgetMethodJavadocMethodTypeParams() throws Exception {
		loadMavenProject(QuteMavenProjectName.qute_quickstart);

		QuteJavadocParams params = new QuteJavadocParams(//
				"org.acme.qute.generic.B", //
				QuteMavenProjectName.qute_quickstart, //
				"get", //
				"get(param : B2) : B1", //
				DocumentFormat.PlainText);

		String actual = QuteSupportForTemplate.getInstance().getJavadoc(params, getJDTUtils(), new EmptyProgressIndicator());
		String expected = " some docs ";
		assertEquals(expected, actual);
	}

}
