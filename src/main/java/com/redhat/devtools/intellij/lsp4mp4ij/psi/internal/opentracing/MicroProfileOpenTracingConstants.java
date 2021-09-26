/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.opentracing;

/**
 * MicroProfile Open Tracing constants
 * 
 * @author David Kwon
 * 
 * @see https://github.com/eclipse/microprofile-opentracing
 */
public class MicroProfileOpenTracingConstants {
	
	public static final String TRACED_ANNOTATION = "org.eclipse.microprofile.opentracing.Traced";
	
	public static final String SKIP_PATTERN = "mp.opentracing.server.skip-pattern";

	public static final String OPERATION_NAME_PROVIDER = "mp.opentracing.server.operation-name-provider";
}