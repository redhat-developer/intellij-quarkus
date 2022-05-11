/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.providers;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;

import java.util.HashSet;
import java.util.Set;

/**
 * The Quarkus context used while building scope process.
 * 
 * @author Angelo ZERR
 *
 */
public class QuarkusContext {

	private static final String QUARKUS_CONTEXT_KEY = QuarkusContext.class.getName();

	private final Set<String> dependenciesToCollect;

	private QuarkusContext() {
		this.dependenciesToCollect = new HashSet<>();
		this.collectDependenciesFor("quarkus-smallrye-openapi-deployment");
	}

	/**
	 * Returns the Quarkus context of the current building scope context.
	 * 
	 * @param context the current building scope context
	 * @return the Quarkus context of the current building scope context.
	 */
	public static QuarkusContext getQuarkusContext(SearchContext context) {
		QuarkusContext quarkusContext = (QuarkusContext) context.get(QUARKUS_CONTEXT_KEY);
		if (quarkusContext == null) {
			quarkusContext = new QuarkusContext();
			context.put(QUARKUS_CONTEXT_KEY, quarkusContext);
		}
		return quarkusContext;
	}

	/**
	 * Collect dependencies for the given quarkus deployment artifact.
	 * 
	 * @param deploymentArtifact the deployment artifact.
	 */
	public void collectDependenciesFor(String deploymentArtifact) {
		dependenciesToCollect.add(deploymentArtifact);
	}

	/**
	 * Returns true if the dependencies from the given deployment artifact must be
	 * downloaded and false otherwise.
	 * 
	 * @param deploymentArtifact
	 * @return true if the dependencies from the given deployment artifact must be
	 *         downloaded and false otherwise.
	 */
	public boolean isCollectDependenciesFor(String deploymentArtifact) {
		return dependenciesToCollect.contains(deploymentArtifact);
	}

}
