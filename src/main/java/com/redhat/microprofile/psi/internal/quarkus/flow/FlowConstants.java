/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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
	 * {@code namespace.PascalCaseName}.
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

}
