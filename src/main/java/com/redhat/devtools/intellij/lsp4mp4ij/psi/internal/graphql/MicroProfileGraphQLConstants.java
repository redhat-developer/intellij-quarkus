/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.graphql;

/**
 * Constants for microprofile-graphql support.
 */
public class MicroProfileGraphQLConstants {

    private MicroProfileGraphQLConstants() {
    }

    public static final String QUERY_ANNOTATION = "org.eclipse.microprofile.graphql.Query";
    public static final String MUTATION_ANNOTATION = "org.eclipse.microprofile.graphql.Mutation";
    public static final String SUBSCRIPTION_ANNOTATION = "io.smallrye.graphql.api.Subscription";
    public static final String GRAPHQL_API_ANNOTATION = "org.eclipse.microprofile.graphql.GraphQLApi";
    public static final String UNION_ANNOTATION = "io.smallrye.graphql.api.Union";
    public static final String DIRECTIVE_ANNOTATION = "io.smallrye.graphql.api.Directive";
    public static final String DIAGNOSTIC_SOURCE = "microprofile-graphql";
    public static final String MULTI = "io.smallrye.mutiny.Multi";
    public static final String FLOW_PUBLISHER = "java.util.concurrent.Flow.Publisher";

}