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

public class QuarkusExtensionsTest {
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
        assertEquals("RESTEasy JAX-RS", model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals("RESTEasy JAX-RS", model.getCategories().get(0).getExtensions().get(0).asLabel());
    }

    @Test
    public void checkPreviewExtension() throws IOException {
        QuarkusModel model = new QuarkusModel(load("/single-preview-extension.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals("Amazon DynamoDB client", model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals("Amazon DynamoDB client (Preview)", model.getCategories().get(0).getExtensions().get(0).asLabel());
    }

    @Test
    public void checkExperimentalExtension() throws IOException {
        QuarkusModel model = new QuarkusModel(load("/single-experimental-extension.json"));
        assertEquals(1, model.getCategories().size());
        assertEquals(1, model.getCategories().get(0).getExtensions().size());
        assertEquals("Amazon DynamoDB client", model.getCategories().get(0).getExtensions().get(0).getName());
        assertEquals("Amazon DynamoDB client (Experimental)", model.getCategories().get(0).getExtensions().get(0).asLabel());
    }
}
