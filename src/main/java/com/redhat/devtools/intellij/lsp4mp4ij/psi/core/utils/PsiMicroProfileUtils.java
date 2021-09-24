/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDT MicroProfile utilities.
 * 
 * @author Angelo ZERR
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/JDTMicroProfileUtils.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/JDTMicroProfileUtils.java</a>
 *
 */
public class PsiMicroProfileUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(PsiMicroProfileUtils.class);

	private PsiMicroProfileUtils() {

	}

	/**
	 * returns the project URI of the given project.
	 * 
	 * @param project the project
	 * @return the project URI of the given project.
	 */
	public static String getProjectURI(Module project) {
		return PsiUtilsLSImpl.getProjectURI(project);
	}

	/**
	 * Returns true if <code>javaProject</code> is a MicroProfile project. Returns
	 * false otherwise.
	 * 
	 * @param javaProject the Java project to check
	 * @return true only if <code>javaProject</code> is a MicroProfile project.
	 */
	public static boolean isMicroProfileProject(Module javaProject) {
		return PsiUtilsLSImpl.getInstance().findClass(javaProject, MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION) != null;
	}
}
