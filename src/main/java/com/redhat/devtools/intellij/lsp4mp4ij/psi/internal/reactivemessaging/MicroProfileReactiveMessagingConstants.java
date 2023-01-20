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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.reactivemessaging;

/**
 * MicroProfile Reactive Messaging constants
 *
 * @author Angelo ZERR
 *
 */
public class MicroProfileReactiveMessagingConstants {

    public MicroProfileReactiveMessagingConstants() {
    }

    // MicroProfile Reactive Messaging annotations
    // See
    // https://github.com/eclipse/microprofile-reactive-messaging/blob/master/api/src/main/java/org/eclipse/microprofile/reactive/messaging

    public static final String MICRO_PROFILE_REACTIVE_MESSAGING_DIAGNOSTIC_SOURCE = "microprofile-reactive-messaging";

    // API
    public static final String CONNECTOR_ANNOTATION = "org.eclipse.microprofile.reactive.messaging.spi.Connector";

    public static final String INCOMING_ANNOTATION = "org.eclipse.microprofile.reactive.messaging.Incoming";

    public static final String OUTGOING_ANNOTATION = "org.eclipse.microprofile.reactive.messaging.Outgoing";

    public static final String CHANNEL_ANNOTATION = "org.eclipse.microprofile.reactive.messaging.Channel";

    public static final String EMITTER_CLASS = "org.eclipse.microprofile.reactive.messaging.Emitter";

    // smallrye
    // See
    // https://github.com/smallrye/smallrye-reactive-messaging/blob/master/api/src/main/java/io/smallrye/reactive/messaging/annotations

    public static final String CONNECTOR_ATTRIBUTES_ANNOTATION = "io.smallrye.reactive.messaging.annotations.ConnectorAttributes";

    public static final String CONNECTOR_ATTRIBUTE_ANNOTATION = "io.smallrye.reactive.messaging.annotations.ConnectorAttribute";

}
