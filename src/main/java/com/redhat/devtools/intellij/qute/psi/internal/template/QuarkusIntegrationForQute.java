/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.template;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.qute.psi.internal.template.datamodel.DataModelProviderRegistry;

import com.redhat.qute.commons.QuteProjectScope;
import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelProject;
import com.redhat.qute.commons.datamodel.DataModelTemplate;
import com.redhat.qute.commons.usertags.UserTagInfo;

/**
 * Support for Quarkus integration for Qute which collect parameters information
 * (a name and a Java type) for Qute template. This collect uses several
 * strategies :
 * 
 * <ul>
 * <li>@CheckedTemplate support: collect parameters for Qute Template by
 * searching @CheckedTemplate annotation.</li>
 * <li>Template field support: collect parameters for Qute Template by searching
 * Template instance declared as field in Java class.</li>
 * <li>Template extension support: see
 * https://quarkus.io/guides/qute-reference#template_extension_methods</li>
 * </ul>
 * 
 * @author Angelo ZERR
 * 
 * @see <a href="https://quarkus.io/guides/qute-reference#quarkus_integration">https://quarkus.io/guides/qute-reference#quarkus_integration</a>
 * @see <a href="https://quarkus.io/guides/qute-reference#typesafe_templates">https://quarkus.io/guides/qute-reference#typesafe_templates</a>
 * @see <a href="https://quarkus.io/guides/qute-reference#template_extension_methods">https://quarkus.io/guides/qute-reference#template_extension_methods</a>
 */
public class QuarkusIntegrationForQute {

	private static final String TEMPLATES_TAGS_ENTRY = "templates/tags";
	private static final Logger LOGGER = Logger.getLogger(QuarkusIntegrationForQute.class.getName());

	public static DataModelProject<DataModelTemplate<DataModelParameter>> getDataModelProject(Module javaProject,
																							  IPsiUtils utils,
																							  ProgressIndicator monitor) {
		return DataModelProviderRegistry.getInstance().getDataModelProject(javaProject,
				QuteProjectScope.SOURCES_AND_DEPENDENCIES, utils, monitor);
	}

	/**
	 * Collect user tags from the given Java project.
	 * 
	 * @param javaProject the Java project.
	 * @param monitor     the progress monitor
	 * 
	 * @return user tags from the given Java project.
	 */
	public static List<UserTagInfo> getUserTags(Module javaProject, ProgressIndicator monitor) {
		List<UserTagInfo> tags = new ArrayList<UserTagInfo>();
		// Loop for each JAR of the classpath and try to collect files from the
		// 'templates.tags' entry
		VirtualFile[] roots = ModuleRootManager.getInstance(javaProject).orderEntries().classes().getRoots();
		for(VirtualFile root : roots) {
			collectUserTags(root, tags);
		}
		return tags;
	}

	/**
	 * Collect user tags for the given package fragment root.
	 * 
	 * @param root the package fragment root.
	 * @param tags the user tags list to fill.
	 */
	private static void collectUserTags(VirtualFile root, List<UserTagInfo> tags) {
		VirtualFile templates = root.findFileByRelativePath(TEMPLATES_TAGS_ENTRY);
		if (templates != null && templates.exists() && templates.isDirectory()) {
			for(VirtualFile child : templates.getChildren()) {
				try {
					if (!child.isDirectory()) {
						String fileName = child.getName();
						String uri = toUri(child);
						String content = convertStreamToString(child.getInputStream());
						UserTagInfo tagInfo = new UserTagInfo();
						tagInfo.setFileName(fileName);
						tagInfo.setUri(uri);
						tagInfo.setContent(content);
						tags.add(tagInfo);
					}
				} catch (IOException e) {
					LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
				}
			}
		}
	}

	/**
	 * Convert the given {@link InputStream} into a String. The source InputStream
	 * will then be closed.
	 * 
	 * @param is the input stream
	 * @return the given input stream in a String.
	 */
	private static String convertStreamToString(InputStream is) {
		try (Scanner s = new Scanner(is)) {
			s.useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";
		}
	}

	// see
	// https://github.com/microsoft/vscode-java-dependency/blob/27c306b770c23b1eba1f9a7c3e70d2793baced68/jdtls.ext/com.microsoft.jdtls.ext.core/src/com/microsoft/jdtls/ext/core/ExtUtils.java#L39

	private static String toUri(VirtualFile jarEntryFile) {
		return LSPIJUtils.toUriAsString(jarEntryFile);
	}
}
