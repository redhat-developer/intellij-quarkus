/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.quarkus.telemetry;

/**
 * Quarkus telemetry event name.
 */
public enum TelemetryEventName {

    // LSP event names
    LSP_START_MICROPROFILE_SERVER(Constants.LSP_PREFIX + "start"),
    LSP_START_QUTE_SERVER(Constants.LSP_PREFIX + "startQute"),

    // Model event names
    MODEL_REMOVE_LIBRARY(Constants.MODEL_PREFIX + "removeLibrary"),
    MODEL_ADD_LIBRARY(Constants.MODEL_PREFIX + "addLibrary"),

    // UI event names
    UI_WIZARD(Constants.UI_PREFIX + "wizard"),
    UI_OPEN_APPLICATION(Constants.UI_PREFIX + "openApplication"),
    UI_OPEN_DEV_UI(Constants.UI_PREFIX + "openDevUI"),

    // Run event names
    RUN_RUN(Constants.RUN_PREFIX + "run");

    private static class Constants {
        public static final String LSP_PREFIX = "lsp-";
        public static final String UI_PREFIX = "ui-";
        public static final String MODEL_PREFIX = "model-";
        public static final String RUN_PREFIX = "run-";
    }

    private final String eventName;

    TelemetryEventName(String eventName) {
        this.eventName = eventName;
    }


    public String getEventName() {
        return eventName;
    }

}