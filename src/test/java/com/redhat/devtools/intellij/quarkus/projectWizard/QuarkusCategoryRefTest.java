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
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class QuarkusCategoryRefTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testDeserializeObjectCategory() throws IOException {
        String json = """
            [{
              "id": "io.quarkus:quarkus-rest",
              "category": {"id": "web", "name": "Web"},
              "name": "Quarkus REST"
            }]
            """;
        List<QuarkusExtension> extensions = mapper.readValue(json,
            new TypeReference<List<QuarkusExtension>>(){});

        assertEquals(1, extensions.size());
        assertNotNull(extensions.get(0).getCategory());
        assertEquals("web", extensions.get(0).getCategory().getId());
        assertEquals("Web", extensions.get(0).getCategory().getName());
        assertEquals("Web", extensions.get(0).getCategoryName());
    }

    @Test
    public void testDeserializeMultipleExtensionsWithObjectCategory() throws IOException {
        String json = """
            [
              {
                "id": "io.quarkus:quarkus-rest",
                "category": {"id": "web", "name": "Web"},
                "name": "Quarkus REST"
              },
              {
                "id": "io.quarkus:quarkus-mongodb",
                "category": {"id": "data", "name": "Data"},
                "name": "MongoDB"
              }
            ]
            """;
        List<QuarkusExtension> extensions = mapper.readValue(json,
            new TypeReference<List<QuarkusExtension>>(){});

        assertEquals(2, extensions.size());
        assertEquals("Web", extensions.get(0).getCategoryName());
        assertEquals("Data", extensions.get(1).getCategoryName());
    }

    @Test
    public void testDeserializeObjectCategoryWithoutNameField() throws IOException {
        String json = """
            [{
              "id": "io.quarkus:quarkus-test",
              "category": {"id": "web"},
              "name": "Test Extension"
            }]
            """;
        List<QuarkusExtension> extensions = mapper.readValue(json,
            new TypeReference<List<QuarkusExtension>>(){});

        assertEquals(1, extensions.size());
        assertNotNull(extensions.get(0).getCategory());
        assertEquals("web", extensions.get(0).getCategory().getId());
        assertNull(extensions.get(0).getCategory().getName());
    }

    @Test
    public void testDeserializeEmptyObjectCategory() throws IOException {
        String json = """
            [{
              "id": "io.quarkus:quarkus-test",
              "category": {},
              "name": "Test Extension"
            }]
            """;
        List<QuarkusExtension> extensions = mapper.readValue(json,
            new TypeReference<List<QuarkusExtension>>(){});

        assertEquals(1, extensions.size());
        assertNotNull(extensions.get(0).getCategory());
    }

    @Test
    public void testDeserializeNullCategory() throws IOException {
        String json = """
            [{
              "id": "io.quarkus:quarkus-test",
              "category": null,
              "name": "Test Extension"
            }]
            """;
        List<QuarkusExtension> extensions = mapper.readValue(json,
            new TypeReference<List<QuarkusExtension>>(){});

        assertEquals(1, extensions.size());
        assertNull(extensions.get(0).getCategory());
        assertNull(extensions.get(0).getCategoryName());
    }

    @Test
    public void testCategoryInExtensionsModel() throws IOException {
        String json = """
            [
              {
                "id": "io.quarkus:quarkus-rest",
                "category": {"id": "web", "name": "Web"},
                "name": "Quarkus REST",
                "order": 0
              },
              {
                "id": "io.quarkus:quarkus-reactive-rest",
                "category": {"id": "web", "name": "Web"},
                "name": "Reactive REST",
                "order": 1
              }
            ]
            """;
        List<QuarkusExtension> extensions = mapper.readValue(json,
            new TypeReference<List<QuarkusExtension>>(){});

        QuarkusExtensionsModel model = new QuarkusExtensionsModel("3.40", extensions);

        assertNotNull(model.getCategories());
        assertEquals(1, model.getCategories().size());
        assertEquals("Web", model.getCategories().get(0).getName());
        assertEquals(2, model.getCategories().get(0).getExtensions().size());
    }

    @Test
    public void testRealWorldQuarkus340Response() throws IOException {
        String json = """
            [{
              "id": "io.quarkus:quarkus-rest",
              "shortId": "rest",
              "version": "3.40.1",
              "name": "Quarkus REST",
              "description": "Create REST endpoints using Quarkus REST",
              "shortName": "rest",
              "category": {"id": "web", "name": "Web"},
              "labels": ["reactive", "rest", "web"],
              "tags": ["reactive", "rest", "web"],
              "status": "stable",
              "guide": "https://quarkus.io/guides/rest",
              "order": 0,
              "default": true,
              "platform": true,
              "providesExampleCode": true
            }]
            """;

        List<QuarkusExtension> extensions = mapper.readValue(json,
            new TypeReference<List<QuarkusExtension>>(){});

        assertEquals(1, extensions.size());
        QuarkusExtension ext = extensions.get(0);
        assertEquals("io.quarkus:quarkus-rest", ext.getId());
        assertEquals("Web", ext.getCategoryName());
        assertEquals("Quarkus REST", ext.getName());
        assertEquals("stable", ext.getStatus());
    }

    @Test
    public void testQuarkusCategoryRefToString() {
        QuarkusCategoryRef ref = new QuarkusCategoryRef("web", "Web");
        assertEquals("Web", ref.toString());

        QuarkusCategoryRef refNoName = new QuarkusCategoryRef("web", null);
        assertEquals("web", refNoName.toString());
    }
}
