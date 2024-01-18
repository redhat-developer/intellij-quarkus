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
import com.redhat.qute.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.*;

/**
 * Quarkus model registry fetching and caching data for Quarkus stream versions and extensions.
 */
public class QuarkusModelRegistry {

    public static class CreateQuarkusProjectRequest {
        public String endpoint;
        public String tool;
        public String groupId;
        public String artifactId;
        public String version;
        public String className;
        public String path;
        public int javaVersion;
        public QuarkusExtensionsModel model;
        public File output;
        public boolean codeStarts;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusModelRegistry.class);
    /**
     * Default request timeout in seconds
     */
    public static final int DEFAULT_TIMEOUT_IN_SEC = 10;
    public static final int DEFAULT_TIMEOUT_IN_MS = DEFAULT_TIMEOUT_IN_SEC*1000;
    private static final String EXTENSIONS_SUFFIX = "/api/extensions/stream/";
    private static final String STREAMS_SUFFIX = "/api/streams";

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
        try {
            return ApplicationManager.getApplication().executeOnPooledThread(() -> loadStreams(endPointURL, indicator)).get(DEFAULT_TIMEOUT_IN_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException|ExecutionException|TimeoutException e) {
            throw new IOException(e);
        }
    }

    public QuarkusModel loadStreams(String endPointURL, ProgressIndicator indicator) throws IOException {
        long start = System.currentTimeMillis();
        String normalizedEndPointURL = normalizeURL(endPointURL);
        indicator.setText("Looking up Quarkus streams from endpoint " + endPointURL);
        QuarkusModel streamModel = models.get(endPointURL);
        if (streamModel != null) {
            return streamModel;
        }
        indicator.setText("Loading Quarkus streams from endpoint " + endPointURL);
        streamModel = HttpRequests.request(normalizedEndPointURL + STREAMS_SUFFIX)
                .connectTimeout(DEFAULT_TIMEOUT_IN_MS)
                .readTimeout(DEFAULT_TIMEOUT_IN_MS)
                .userAgent(USER_AGENT)
                .tuner(request -> {
                    request.setRequestProperty(CODE_QUARKUS_IO_CLIENT_NAME_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_NAME_HEADER_VALUE);
                    request.setRequestProperty(CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_VALUE);
                })
                .connect(request -> {
                    try (Reader reader = request.getReader(indicator)) {
                        List<QuarkusStream> streams = mapper.readValue(reader, new TypeReference<List<QuarkusStream>>() {
                        });
                        QuarkusModel model = new QuarkusModel(normalizedEndPointURL, streams);
                        long elapsed = System.currentTimeMillis() - start;
                        LOGGER.info("Loaded Quarkus streams in {} ms", elapsed);
                        return model;
                    }
                });
        models.put(endPointURL, streamModel);
        return streamModel;
    }

    private static String normalizeURL(String endPointURL) {
        endPointURL = endPointURL.trim();
        while (endPointURL.endsWith("/")) {
            endPointURL = endPointURL.substring(0, endPointURL.length() - 1);
        }
        return endPointURL;
    }

    public static QuarkusExtensionsModel loadExtensionsModel(String endPointURL, String key, ProgressIndicator indicator) throws IOException {
        long start = System.currentTimeMillis();
        String normalizedEndPointURL = normalizeURL(endPointURL);
        indicator.setText("Looking up Quarkus extensions from endpoint " + endPointURL + " and key " + key);
        String query = normalizedEndPointURL + EXTENSIONS_SUFFIX + key + "?" + PLATFORM_ONLY_PARAMETER + "=false";
        return HttpRequests.request(query).userAgent(USER_AGENT).tuner(request -> {
                request.setRequestProperty(CODE_QUARKUS_IO_CLIENT_NAME_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_NAME_HEADER_VALUE);
                request.setRequestProperty(CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_VALUE);
            }).connect(request -> {
                try (Reader reader = request.getReader(indicator)) {
                    List<QuarkusExtension> extensions = mapper.readValue(reader, new TypeReference<>() {
                    });
                    QuarkusExtensionsModel newModel = new QuarkusExtensionsModel(key, extensions);
                    long elapsed = System.currentTimeMillis() - start;
                    LOGGER.info("Loaded Quarkus extensions in {} ms", elapsed);
                    return newModel;
                } catch (IOException e) {
                    throw new ProcessCanceledException(e);
                }
        });
    }

    public static void zip(CreateQuarkusProjectRequest createQuarkusProjectRequest) throws IOException {
        Url url = Urls.newFromEncoded(normalizeURL(createQuarkusProjectRequest.endpoint) + "/api/download");
        String body = buildParameters(createQuarkusProjectRequest);
        RequestBuilder builder = HttpRequests.post(url.toString(), HttpRequests.JSON_CONTENT_TYPE).userAgent(QuarkusModelRegistry.USER_AGENT).tuner(connection -> {
            connection.setRequestProperty(CODE_QUARKUS_IO_CLIENT_NAME_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_NAME_HEADER_VALUE);
            connection.setRequestProperty(CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_VALUE);
        });
        try {
            if (ApplicationManager.getApplication().executeOnPooledThread(() -> builder.connect(request -> {
                request.write(body);
                ZipUtil.unpack(request.getInputStream(), createQuarkusProjectRequest.output, name -> {
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

    private static String buildParameters(CreateQuarkusProjectRequest createQuarkusProjectRequest) {
        JsonObject json = new JsonObject();

        json.addProperty(CODE_TOOL_PARAMETER_NAME, createQuarkusProjectRequest.tool);
        json.addProperty(CODE_GROUP_ID_PARAMETER_NAME, createQuarkusProjectRequest.groupId);
        json.addProperty(CODE_ARTIFACT_ID_PARAMETER_NAME, createQuarkusProjectRequest.artifactId);
        json.addProperty(CODE_VERSION_PARAMETER_NAME, createQuarkusProjectRequest.version);
        if (!StringUtils.isEmpty(createQuarkusProjectRequest.className)) {
            json.addProperty(CODE_CLASSNAME_PARAMETER_NAME, createQuarkusProjectRequest.className);
        }
        if (!StringUtils.isEmpty(createQuarkusProjectRequest.path)) {
            json.addProperty(CODE_PATH_PARAMETER_NAME, createQuarkusProjectRequest.path);
        }
        if (createQuarkusProjectRequest.javaVersion > 0) {
            json.addProperty(CODE_JAVA_VERSION_PARAMETER_NAME, createQuarkusProjectRequest.javaVersion);
        }
        if (!createQuarkusProjectRequest.codeStarts) {
            json.addProperty(CODE_NO_EXAMPLES_NAME, CODE_NO_EXAMPLES_DEFAULT);
        }
        JsonArray extensions = new JsonArray();
        createQuarkusProjectRequest.model.getCategories().stream().flatMap(category -> category.getExtensions().stream()).
                filter(extension -> extension.isSelected() || extension.isDefaultExtension()).
                forEach(extension -> extensions.add(extension.getId()));
        json.add(CODE_EXTENSIONS_PARAMETER_NAME, extensions);
        json.addProperty(CODE_STREAM_PARAMETER_NAME, createQuarkusProjectRequest.model.getKey());
        return json.toString();
    }

    public void reset() {
        models.clear();
    }
}
