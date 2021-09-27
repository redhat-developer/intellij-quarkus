/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics;

/**
 * MicroProfile Metrics constants
 * 
 * @author David Kwon
 *
 */
public class MicroProfileMetricsConstants {

	public static final String METRIC_ID = "org.eclipse.microprofile.metrics.MetricID";

	public static final String GAUGE_ANNOTATION = "org.eclipse.microprofile.metrics.annotation.Gauge";

	public static final String DIAGNOSTIC_SOURCE = "microprofile-metrics";

	// CDI Scope Annotations
	public static final String APPLICATION_SCOPED_ANNOTATION = "javax.enterprise.context.ApplicationScoped";
	public static final String REQUEST_SCOPED_ANNOTATION = "javax.enterprise.context.RequestScoped";
	public static final String SESSION_SCOPED_ANNOTATION = "javax.enterprise.context.SessionScoped";
	public static final String DEPENDENT_ANNOTATION = "javax.enterprise.context.Dependent";

	public static final String REQUEST_SCOPED_ANNOTATION_NAME = "RequestScoped";
	public static final String SESSION_SCOPED_ANNOTATION_NAME = "SessionScoped";
	public static final String DEPENDENT_ANNOTATION_NAME = "Dependent";
}
