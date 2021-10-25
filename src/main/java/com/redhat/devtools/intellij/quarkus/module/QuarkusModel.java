/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.module;

import com.intellij.openapi.progress.ProgressIndicator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuarkusModel {
    private String baseURL;

    private List<QuarkusStream> streams;

    private Map<String, QuarkusExtensionsModel> extensionsModelMap = new HashMap<>();

    public QuarkusModel(String baseURL, List<QuarkusStream> streams) {
        this.baseURL = baseURL;
        this.streams = streams;
    }
    public List<QuarkusStream> getStreams() {
        return streams;
    }

    public QuarkusExtensionsModel getExtensionsModel(String key, ProgressIndicator indicator) throws IOException {
        QuarkusExtensionsModel extensionsModel = extensionsModelMap.get(key);
        if (extensionsModel == null) {
            extensionsModel = QuarkusModelRegistry.loadExtensionsModel(baseURL, key, indicator);
            extensionsModelMap.put(key, extensionsModel);
        }
        return extensionsModel;
    }
}
