/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.flow;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;

/**
 * Quarkus Flow constants.
 *
 * @see <a href="https://docs.quarkiverse.io/quarkus-flow/dev/workflow-definitions.html">Quarkus Flow workflow definitions</a>
 */
public class FlowConstants {

	private FlowConstants() {
	}

	/**
	 * The generic CDI qualifier used to select a bean by name, e.g.
	 * {@code @Identifier("namespace:name")}.
	 */
	public static final String IDENTIFIER_ANNOTATION = "io.smallrye.common.annotation.Identifier";

	/**
	 * The {@code @Identifier} annotation member holding the workflow identifier, e.g.
	 * {@code @Identifier(value = "namespace:name")} / {@code @Identifier("namespace:name")}.
	 */
	public static final String VALUE_ANNOTATION_MEMBER = "value";

	/**
	 * Injected workflow bean as WorkflowDefinition {@code @Inject @Identifier("...") WorkflowDefinition workflowDefinition;}.
	 */
	public static final String WORKFLOW_DEFINITION_CLASS = "io.serverlessworkflow.impl.WorkflowDefinition";
	/**
	 * Injected workflow bean as Flow {@code @Inject @Identifier("...") Flow flow;}.
	 */
	public static final String FLOW_CLASS = "io.quarkiverse.flow.Flow";

	/**
	 * Property which configures the directory (relative to src/main/resources) where workflow
	 * files are looked up. Default is {@link #DEFAULT_FLOW_DEFINITIONS_DIR}.
	 */
	public static final String FLOW_DEFINITIONS_DIR_PROPERTY = "quarkus.flow.definitions.dir";

	public static final String DEFAULT_FLOW_DEFINITIONS_DIR = "flow";

	/**
	 * Property which configures how the {@code @Identifier} value is derived from a workflow's
	 * {@code document} metadata: {@link #NAMING_STRATEGY_SPEC} (default) or
	 * {@link #NAMING_STRATEGY_CLASS}.
	 */
	public static final String FLOW_NAMING_STRATEGY_PROPERTY = "quarkus.flow.definitions.naming-strategy";

	/**
	 * {@code namespace:name} or {@code namespace:name:version}.
	 */
	public static final String NAMING_STRATEGY_SPEC = "spec";

	/**
	 * {@code namespace.PascalCaseName} for unversioned workflows, or
	 * {@code namespace.vX_Y_Z.PascalCaseName} for versioned workflows.
	 */
	public static final String NAMING_STRATEGY_CLASS = "class";

	/**
	 * Property which controls whether symlinked workflow files/directories are followed during
	 * discovery. Default is <code>false</code>.
	 */
	public static final String FLOW_FOLLOW_SYMLINKS_PROPERTY = "quarkus.flow.definitions.follow-symlinks";

	/**
	 * File extensions scanned when discovering workflow files.
	 */
	public static final String[] SUPPORTED_EXTENSIONS = { ".yaml", ".yml", ".json" };

	/**
	 * Returns <code>true</code> if the given type is a Quarkus Flow injection point type -
	 * either the higher-level {@link #FLOW_CLASS} or the lower-level
	 * {@link #WORKFLOW_DEFINITION_CLASS} - and <code>false</code> otherwise.
	 */
	public static boolean isFlowType(PsiType type) {
		if (type instanceof PsiClassType classType) {
			PsiClass clazz = classType.resolve();
			if (clazz == null) {
				return false;
			}
			String qualifiedName = clazz.getQualifiedName();
			return FLOW_CLASS.equals(qualifiedName) || WORKFLOW_DEFINITION_CLASS.equals(qualifiedName);
		}
		return false;
	}

}
