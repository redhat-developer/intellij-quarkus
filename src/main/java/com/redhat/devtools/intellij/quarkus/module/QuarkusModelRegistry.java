/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.module;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.io.HttpRequests;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_VALUE;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_QUARKUS_IO_CLIENT_NAME_HEADER_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_QUARKUS_IO_CLIENT_NAME_HEADER_VALUE;

public class QuarkusModelRegistry {
    private static final String EXTENSIONS_SUFFIX = "/api/extensions";

    public static final QuarkusModelRegistry INSTANCE = new QuarkusModelRegistry();

    private final Map<String, QuarkusModel> models = new HashMap<>();

    private static final ObjectMapper mapper = new ObjectMapper();

    static final String USER_AGENT = computeUserAgent();

    private static String computeUserAgent() {
        StringBuilder builder = new StringBuilder(ApplicationInfo.getInstance().getBuild().getProductCode());
        builder.append('/').append(ApplicationInfo.getInstance().getBuild().asStringWithoutProductCodeAndSnapshot());
        builder.append(" (").append(System.getProperty("os.name")).append("; ");
        builder.append(System.getProperty("os.version")).append("; ");
        builder.append(System.getProperty("os.arch")).append("; ");
        builder.append("Java ").append(System.getProperty("java.version")).append(')');
        return builder.toString();
    }

    public QuarkusModel load(String endPointURL, ProgressIndicator indicator) throws IOException {
        indicator.setText("Looking up Quarkus model from endpoint " + endPointURL);
        QuarkusModel model = models.get(endPointURL);
        if (model == null) {
            indicator.setText("Loading Quarkus model from endpoint " + endPointURL);
            try {
                model = ApplicationManager.getApplication().executeOnPooledThread(() -> HttpRequests.request(endPointURL + EXTENSIONS_SUFFIX).userAgent(USER_AGENT).tuner(request -> {
                    request.setRequestProperty(CODE_QUARKUS_IO_CLIENT_NAME_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_NAME_HEADER_VALUE);
                    request.setRequestProperty(CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_VALUE);
                }).connect(request -> {
                        try (Reader reader = request.getReader(indicator)) {
                            List<QuarkusExtension> extensions = mapper.readValue(reader, new TypeReference<List<QuarkusExtension>>() {
                            });
                            QuarkusModel newModel = new QuarkusModel(extensions);
                            return newModel;
                        }
                })).get();
            } catch (InterruptedException|ExecutionException e) {
                throw new IOException(e);
            }
        }
        return model;
    }
}
