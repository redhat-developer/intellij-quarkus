/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
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
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Integration test that verifies Code Quarkus API schema.
 *
 * Fetches actual extensions from <a href="https://code.quarkus.io/api">...</a>
 * and verifies deserialization into QuarkusExtension model.
 *
 * Will fail if API schema changes (e.g., category field structure).
 * Safeguard against issue #1626 happening again.
 */
@RunWith(Parameterized.class)
public class QuarkusExtensionApiSchemaTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String API_BASE = "https://code.quarkus.io/api";
    private static HttpClient httpClient;

    @BeforeClass
    public static void setupHttpClient() {
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @AfterClass
    public static void closeHttpClient() {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "io.quarkus.platform:3.27" },
            { "io.quarkus.platform:3.33" },
            { "io.quarkus.platform:3.40" },
            { "io.quarkus.platform:4.0" }
        });
    }

    private final String streamKey;

    public QuarkusExtensionApiSchemaTest(String streamKey) {
        this.streamKey = streamKey;
    }

    /**
     * Fetches extensions from the API and verifies deserialization works.
     *
     * This test will fail if:
     * - API is unreachable
     * - API response schema has changed
     * - QuarkusExtension model doesn't match API structure
     */
    @Test
    public void testExtensionApiSchema() throws Exception {
        String url = API_BASE + "/extensions/stream/" + streamKey + "?platformOnly=false";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        List<QuarkusExtension> extensions = mapper.readValue(response.body(),
          new TypeReference<>() {
          });

        assertNotNull("Extensions should not be null for " + streamKey, extensions);
        assertFalse("Extensions list should not be empty for " + streamKey, extensions.isEmpty());

        QuarkusExtension first = extensions.get(0);
        assertNotNull(
            "Category should not be null for " + streamKey + ". " +
            "If this fails, API schema changed!",
            first.getCategory());
        assertNotNull(
            "Category name should not be null for " + streamKey,
            first.getCategory().getName());
    }
}