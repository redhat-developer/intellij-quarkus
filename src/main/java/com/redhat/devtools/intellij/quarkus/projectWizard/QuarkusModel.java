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
package com.redhat.devtools.intellij.quarkus.projectWizard;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class QuarkusModel implements Disposable {
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

    //Used in Unit test
    public QuarkusExtensionsModel getExtensionsModel(String key, ProgressIndicator indicator) throws IOException {
        try {
            return ApplicationManager.getApplication().executeOnPooledThread(() -> loadExtensionsModel(key, indicator)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    public QuarkusExtensionsModel loadExtensionsModel(String key, ProgressIndicator indicator) throws IOException {
        QuarkusExtensionsModel extensionsModel = extensionsModelMap.get(key);
        if (extensionsModel == null) {
            extensionsModel = QuarkusModelRegistry.loadExtensionsModel(baseURL, key, indicator);
            extensionsModelMap.put(key, extensionsModel);
        }
        return extensionsModel;
    }

    @Override
    public void dispose() {
        extensionsModelMap.clear();
    }
}
