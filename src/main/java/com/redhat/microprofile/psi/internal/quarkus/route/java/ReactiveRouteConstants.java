/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.route.java;

/**
 * Reactive route constants.
 *
 * @see <a href="https://quarkus.io/guides/reactive-routes#declaring-reactive-routes">https://quarkus.io/guides/reactive-routes#declaring-reactive-routes</a>
 */
public class ReactiveRouteConstants {

    // ---------- @io.quarkus.vertx.web.RouteBase

    public static final String ROUTE_BASE_FQN = "io.quarkus.vertx.web.RouteBase";

    public static final String ROUTE_BASE_PATH = "path";

    // ---------- @io.quarkus.vertx.web.Route

    public static final String ROUTE_FQN = "io.quarkus.vertx.web.Route";

    public static final String ROUTE_PATH = "path";

    public static final String ROUTE_METHODS = "methods";

}