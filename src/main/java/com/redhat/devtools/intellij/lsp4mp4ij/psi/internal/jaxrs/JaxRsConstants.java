/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.jaxrs;

import java.util.Arrays;
import java.util.List;

/**
 * JAX-RS and jakarta.ws.rs constants.
 *
 * @author Angelo ZERR
 *
 */
public class JaxRsConstants {

	public JaxRsConstants() {
	}

	public static final String JAVAX_WS_RS_PATH_ANNOTATION = "javax.ws.rs.Path";
	public static final String JAKARTA_WS_RS_PATH_ANNOTATION = "jakarta.ws.rs.Path";
	public static final String PATH_VALUE = "value";
	public static final String JAVAX_WS_RS_APPLICATIONPATH_ANNOTATION = "javax.ws.rs.ApplicationPath";
	public static final String JAKARTA_WS_RS_APPLICATIONPATH_ANNOTATION = "jakarta.ws.rs.ApplicationPath";
	public static final String JAVAX_WS_RS_GET_ANNOTATION = "javax.ws.rs.GET";
	public static final String JAVAX_WS_RS_POST_ANNOTATION = "javax.ws.rs.POST";
	public static final String JAVAX_WS_RS_PUT_ANNOTATION = "javax.ws.rs.PUT";
	public static final String JAVAX_WS_RS_DELETE_ANNOTATION = "javax.ws.rs.DELETE";
	public static final String JAVAX_WS_RS_HEAD_ANNOTATION = "javax.ws.rs.HEAD";
	public static final String JAVAX_WS_RS_OPTIONS_ANNOTATION = "javax.ws.rs.OPTIONS";
	public static final String JAVAX_WS_RS_PATCH_ANNOTATION = "javax.ws.rs.PATCH";
	public static final String JAVAX_WS_RS_RESPONSE_TYPE = "javax.ws.rs.core.Response";

	// jakarta.ws.rs HTTP method annotations
	public static final String JAKARTA_WS_RS_GET_ANNOTATION = "jakarta.ws.rs.GET";
	public static final String JAKARTA_WS_RS_POST_ANNOTATION = "jakarta.ws.rs.POST";
	public static final String JAKARTA_WS_RS_PUT_ANNOTATION = "jakarta.ws.rs.PUT";
	public static final String JAKARTA_WS_RS_DELETE_ANNOTATION = "jakarta.ws.rs.DELETE";
	public static final String JAKARTA_WS_RS_HEAD_ANNOTATION = "jakarta.ws.rs.HEAD";
	public static final String JAKARTA_WS_RS_OPTIONS_ANNOTATION = "jakarta.ws.rs.OPTIONS";
	public static final String JAKARTA_WS_RS_PATCH_ANNOTATION = "jakarta.ws.rs.PATCH";

	/**
	 * A list of the fully qualified names of all jax-rs and jakarta.ws.rs http
	 * method annotations.
	 */
	public static final String[] HTTP_METHOD_ANNOTATIONS = { //
	JAVAX_WS_RS_GET_ANNOTATION, //
	JAVAX_WS_RS_POST_ANNOTATION, //
	JAVAX_WS_RS_PUT_ANNOTATION, //
	JAVAX_WS_RS_DELETE_ANNOTATION, //
	JAVAX_WS_RS_HEAD_ANNOTATION, //
	JAVAX_WS_RS_OPTIONS_ANNOTATION, //
	JAVAX_WS_RS_PATCH_ANNOTATION, //
	JAKARTA_WS_RS_GET_ANNOTATION, //
	JAKARTA_WS_RS_POST_ANNOTATION, //
	JAKARTA_WS_RS_PUT_ANNOTATION, //
	JAKARTA_WS_RS_DELETE_ANNOTATION, //
	JAKARTA_WS_RS_HEAD_ANNOTATION, //
	JAKARTA_WS_RS_OPTIONS_ANNOTATION, //
	JAKARTA_WS_RS_PATCH_ANNOTATION };
}
