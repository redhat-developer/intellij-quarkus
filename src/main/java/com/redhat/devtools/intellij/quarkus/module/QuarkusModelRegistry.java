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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.RequestBuilder;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_ARTIFACT_ID_PARAMETER_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_CLASSNAME_PARAMETER_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_EXTENSIONS_PARAMETER_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_GROUP_ID_PARAMETER_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_NO_EXAMPLES_DEFAULT;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_NO_EXAMPLES_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_PATH_PARAMETER_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_VALUE;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_QUARKUS_IO_CLIENT_NAME_HEADER_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_QUARKUS_IO_CLIENT_NAME_HEADER_VALUE;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_TOOL_PARAMETER_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_VERSION_PARAMETER_NAME;

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
        String normalizedEndPointURL = normalizeURL(endPointURL);
        indicator.setText("Looking up Quarkus model from endpoint " + endPointURL);
        QuarkusModel model = models.get(endPointURL);
        if (model == null) {
            indicator.setText("Loading Quarkus model from endpoint " + endPointURL);
            try {
                model = ApplicationManager.getApplication().executeOnPooledThread(() -> HttpRequests.request(normalizedEndPointURL + EXTENSIONS_SUFFIX).userAgent(USER_AGENT).tuner(request -> {
                    request.setRequestProperty(CODE_QUARKUS_IO_CLIENT_NAME_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_NAME_HEADER_VALUE);
                    request.setRequestProperty(CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_VALUE);
                }).connect(request -> {
                        try (Reader reader = request.getReader(indicator)) {
                            List<QuarkusExtension> extensions = mapper.readValue(reader, new TypeReference<List<QuarkusExtension>>() {
                            });
                            QuarkusModel newModel = new QuarkusModel(extensions);
                            return newModel;
                        } catch (IOException e) {
                            throw new ProcessCanceledException(e);
                        }
                })).get();
            } catch (InterruptedException|ExecutionException e) {
                throw new IOException(e);
            }
        }
        if (model == null) {
            throw new IOException();
        }
        return model;
    }

    private static String normalizeURL(String endPointURL) {
        endPointURL = endPointURL.trim();
        while (endPointURL.endsWith("/")) {
            endPointURL = endPointURL.substring(0, endPointURL.length() - 1);
        }
        return endPointURL;
    }

    public static void zip(String endpoint, String tool, String groupId, String artifactId, String version,
                           String className, String path, QuarkusModel model, File output, boolean codeStarts) throws IOException {
        Url url = Urls.newFromEncoded(normalizeURL(endpoint) + "/api/download");
        String body = buildParameters(tool, groupId, artifactId, version, className, path, model, codeStarts);
        RequestBuilder builder = HttpRequests.post(url.toString(), HttpRequests.JSON_CONTENT_TYPE).userAgent(QuarkusModelRegistry.USER_AGENT).tuner(connection -> {
            connection.setRequestProperty(CODE_QUARKUS_IO_CLIENT_NAME_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_NAME_HEADER_VALUE);
            connection.setRequestProperty(CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_VALUE);
        });
        try {
            if (ApplicationManager.getApplication().executeOnPooledThread(() -> builder.connect(request -> {
                request.write(body);
                ZipUtil.unpack(request.getInputStream(), output, name -> {
                    int index = name.indexOf('/');
                    return name.substring(index);
                });
                return true;
            })).get() == null) {
                throw new IOException();
            }
    } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    private static String buildParameters(String tool, String groupId, String artifactId, String version, String className, String path, QuarkusModel model, boolean codeStarts) {
        JsonObject json = new JsonObject();

        json.addProperty(CODE_TOOL_PARAMETER_NAME, tool);
        json.addProperty(CODE_GROUP_ID_PARAMETER_NAME, groupId);
        json.addProperty(CODE_ARTIFACT_ID_PARAMETER_NAME, artifactId);
        json.addProperty(CODE_VERSION_PARAMETER_NAME, version);
        json.addProperty(CODE_CLASSNAME_PARAMETER_NAME, className);
        json.addProperty(CODE_PATH_PARAMETER_NAME, path);
        if (!codeStarts) {
            json.addProperty(CODE_NO_EXAMPLES_NAME, CODE_NO_EXAMPLES_DEFAULT);
        }
        JsonArray extensions = new JsonArray();
        model.getCategories().stream().flatMap(category -> category.getExtensions().stream()).
                filter(extension -> extension.isSelected() || extension.isDefaultExtension()).
                forEach(extension -> extensions.add(extension.getId()));
        json.add(CODE_EXTENSIONS_PARAMETER_NAME, extensions);
        return json.toString();
    }

    public static void zip(String endpoint, String tool, String groupId, String artifactId, String version,
                           String className, String path, QuarkusModel model, File output) throws IOException {
        zip(endpoint, tool, groupId, artifactId, version, className, path, model, output, true);
    }

    }
