/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
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
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuarkusExtensionsTest {
    private static final String AMAZON_DYNAMODB_CLIENT_EXPERIMENTAL_LABEL = "Amazon DynamoDB client (Experimental)";
    private static final String AMAZON_DYNAMODB_CLIENT_PREVIEW_LABEL = "Amazon DynamoDB client (Preview)";
    private static final String AMAZON_DYNAMODB_CLIENT_EXTENSION_NAME = "Amazon DynamoDB client";
    private static final String RESTEASY_JAX_RS_EXTENSION_NAME = "RESTEasy JAX-RS";

    private static ObjectMapper mapper = new ObjectMapper();

    private List<QuarkusExtension> load(String resource) throws IOException {
        return mapper.readValue(QuarkusExtensionsTest.class.getResourceAsStream(resource), new TypeReference<List<QuarkusExtension>>() {
        });
    }

    @Test
    public void checkStableExtension() throws IOException {
        QuarkusModel model = new QuarkusModel(load("/single-stable-extension.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).asLabel());
    }

    @Test
    public void checkPreviewExtension() throws IOException {
        QuarkusModel model = new QuarkusModel(load("/single-preview-extension.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(AMAZON_DYNAMODB_CLIENT_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(AMAZON_DYNAMODB_CLIENT_PREVIEW_LABEL, model.getCategories().get(0).getExtensions().get(0).asLabel());
    }

    @Test
    public void checkExperimentalExtension() throws IOException {
        QuarkusModel model = new QuarkusModel(load("/single-experimental-extension.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(AMAZON_DYNAMODB_CLIENT_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(AMAZON_DYNAMODB_CLIENT_EXPERIMENTAL_LABEL, model.getCategories().get(0).getExtensions().get(0).asLabel());
    }

    @Test
    public void checkExtensionWithDefault() throws IOException {
        QuarkusModel model = new QuarkusModel(load("/single-extension-with-default.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).asLabel());
        assertTrue(model.getCategories().get(0).getExtensions().get(0).isDefaultExtension());
    }

    @Test
    public void checkExtensionWithoutDefault() throws IOException {
        QuarkusModel model = new QuarkusModel(load("/single-extension-without-default.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).asLabel());
        assertFalse(model.getCategories().get(0).getExtensions().get(0).isDefaultExtension());
    }
}
