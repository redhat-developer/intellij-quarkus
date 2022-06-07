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
    private static final String AMAZON_DYNAMODB_CLIENT_EXPERIMENTAL_LABEL = "Amazon DynamoDB client [experimental]";
    private static final String AMAZON_DYNAMODB_CLIENT_PREVIEW_LABEL = "Amazon DynamoDB client [preview]";
    private static final String AMAZON_DYNAMODB_CLIENT_EXTENSION_NAME = "Amazon DynamoDB client";
    private static final String RESTEASY_JAX_RS_EXTENSION_NAME = "RESTEasy JAX-RS";

    private static ObjectMapper mapper = new ObjectMapper();

    private List<QuarkusExtension> load(String resource) throws IOException {
        return mapper.readValue(QuarkusExtensionsTest.class.getResourceAsStream(resource), new TypeReference<List<QuarkusExtension>>() {
        });
    }

    @Test
    public void checkExtensionWithShortId() throws IOException {
        QuarkusExtensionsModel model = new QuarkusExtensionsModel("", load("/single-extension.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals("98e", model.getCategories().get(0).getExtensions().get(0).getShortId());
    }

    @Test
    public void checkStableExtensionWithStatus() throws IOException {
        QuarkusExtensionsModel model = new QuarkusExtensionsModel("", load("/single-stable-extension-with-status.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).asLabel());
    }

    @Test
    public void checkPreviewExtensionWithStatus() throws IOException {
        QuarkusExtensionsModel model = new QuarkusExtensionsModel("", load("/single-preview-extension-with-status.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(AMAZON_DYNAMODB_CLIENT_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(AMAZON_DYNAMODB_CLIENT_PREVIEW_LABEL, model.getCategories().get(0).getExtensions().get(0).asLabel());
    }

    @Test
    public void checkExperimentalExtensionWithStatus() throws IOException {
        QuarkusExtensionsModel model = new QuarkusExtensionsModel("", load("/single-experimental-extension-with-status.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(AMAZON_DYNAMODB_CLIENT_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(AMAZON_DYNAMODB_CLIENT_EXPERIMENTAL_LABEL, model.getCategories().get(0).getExtensions().get(0).asLabel());
    }

    @Test
    public void checkStableExtensionWithTags() throws IOException {
        QuarkusExtensionsModel model = new QuarkusExtensionsModel("", load("/single-stable-extension-with-tags.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).asLabel());
    }

    @Test
    public void checkPreviewExtensionWithTags() throws IOException {
        QuarkusExtensionsModel model = new QuarkusExtensionsModel("", load("/single-preview-extension-with-tags.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(AMAZON_DYNAMODB_CLIENT_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(AMAZON_DYNAMODB_CLIENT_PREVIEW_LABEL, model.getCategories().get(0).getExtensions().get(0).asLabel());
    }

    @Test
    public void checkExperimentalExtensionWithTags() throws IOException {
        QuarkusExtensionsModel model = new QuarkusExtensionsModel("", load("/single-experimental-extension-with-tags.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(AMAZON_DYNAMODB_CLIENT_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(AMAZON_DYNAMODB_CLIENT_EXPERIMENTAL_LABEL, model.getCategories().get(0).getExtensions().get(0).asLabel());
    }

    @Test
    public void checkExtensionWithSeveralTags() throws IOException {
        QuarkusExtensionsModel model = new QuarkusExtensionsModel("", load("/single-extension-with-several-tags.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(AMAZON_DYNAMODB_CLIENT_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals("Amazon DynamoDB client [preview,experimental]", model.getCategories().get(0).getExtensions().get(0).asLabel());
    }


    @Test
    public void checkExtensionWithDefault() throws IOException {
        QuarkusExtensionsModel model = new QuarkusExtensionsModel("", load("/single-extension-with-default.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).asLabel());
        assertTrue(model.getCategories().get(0).getExtensions().get(0).isDefaultExtension());
    }

    @Test
    public void checkExtensionWithoutDefault() throws IOException {
        QuarkusExtensionsModel model = new QuarkusExtensionsModel("", load("/single-extension-without-default.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).asLabel());
        assertFalse(model.getCategories().get(0).getExtensions().get(0).isDefaultExtension());
    }

    @Test
    public void checkExtensionWithProvidesExampleCode() throws IOException {
        QuarkusExtensionsModel model = new QuarkusExtensionsModel("", load("/single-extension-with-provides-example-code.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).asLabel());
        assertTrue(model.getCategories().get(0).getExtensions().get(0).isProvidesExampleCode());
    }

    @Test
    public void checkExtensionWithoutProvidesExampleCode() throws IOException {
        QuarkusExtensionsModel model = new QuarkusExtensionsModel("", load("/single-extension-without-provides-example-code.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals(RESTEASY_JAX_RS_EXTENSION_NAME, model.getCategories().get(0).getExtensions().get(0).asLabel());
        assertFalse(model.getCategories().get(0).getExtensions().get(0).isProvidesExampleCode());
    }
}
