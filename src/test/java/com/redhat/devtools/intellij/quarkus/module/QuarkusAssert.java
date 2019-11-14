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

import com.redhat.quarkus.commons.ExtendedConfigDescriptionBuildItem;
import org.junit.Assert;

import java.util.List;
import java.util.stream.Collectors;

public class QuarkusAssert {
    /**
     * Assert Quarkus properties.
     *
     * @param items    the Quarkus project information items
     * @param expected the expected Quarkus properties.
     */
    public static void assertProperties(List<ExtendedConfigDescriptionBuildItem> items, ExtendedConfigDescriptionBuildItem... expected) {
        assertProperties(items, null, expected);
    }

    /**
     * Assert Quarkus properties.
     *
     * @param items         the Quarkus project information items
     * @param expectedCount Quarkus properties expected count.
     * @param expected      the expected Quarkus properties.
     */
    public static void assertProperties(List<ExtendedConfigDescriptionBuildItem> items, Integer expectedCount,
                                        ExtendedConfigDescriptionBuildItem... expected) {
        if (expectedCount != null) {
            Assert.assertEquals(expectedCount.intValue(), items.size());
        }
        for (ExtendedConfigDescriptionBuildItem item : expected) {
            assertProperty(items, item);
        }
    }

    /**
     * Assert Quarkus property
     *
     * @param items    the Quarkus project information items
     * @param expected the Quarkus property.
     */
    private static void assertProperty(List<ExtendedConfigDescriptionBuildItem> items, ExtendedConfigDescriptionBuildItem expected) {
        List<ExtendedConfigDescriptionBuildItem> matches = items.stream().filter(completion -> {
            return expected.getPropertyName().equals(completion.getPropertyName());
        }).collect(Collectors.toList());

        Assert.assertEquals(
                expected.getPropertyName() + " should only exist once: Actual: "
                        + items.stream().map(c -> c.getPropertyName()).collect(Collectors.joining(",")),
                1, matches.size());

        ExtendedConfigDescriptionBuildItem actual = matches.get(0);
        Assert.assertEquals("Test 'extension name' for '" + expected.getPropertyName() + "'",
                expected.getExtensionName(), actual.getExtensionName());
        Assert.assertEquals("Test 'type' for '" + expected.getPropertyName() + "'", expected.getType(),
                actual.getType());
        Assert.assertEquals("Test 'docs' for '" + expected.getPropertyName() + "'", expected.getDocs(),
                actual.getDocs());
        Assert.assertEquals("Test 'location' for '" + expected.getPropertyName() + "'",
                expected.getLocation().replace('\\', '/'), actual.getLocation().replace('\\', '/'));
        Assert.assertEquals("Test 'source' for '" + expected.getPropertyName() + "'", expected.getSource(),
                actual.getSource());
        Assert.assertEquals("Test 'phase' for '" + expected.getPropertyName() + "'", expected.getPhase(),
                actual.getPhase());
        Assert.assertEquals("Test 'default value' for '" + expected.getPropertyName() + "'", expected.getDefaultValue(),
                actual.getDefaultValue());
    }

    /**
     * Returns an instance of Quarkus property.
     *
     * @param extensionName Quarkus extension name
     * @param propertyName  the property name
     * @param type          the property class type
     * @param docs          the Javadoc
     * @param location      the location (JAR, sources)
     * @param source        the source (class + field)
     * @param phase         the ConfigPhase.
     * @param defaultValue  the default value
     * @return
     */
    public static ExtendedConfigDescriptionBuildItem p(String extensionName, String propertyName, String type,
                                                       String docs, String location, String source, int phase, String defaultValue) {
        ExtendedConfigDescriptionBuildItem item = new ExtendedConfigDescriptionBuildItem();
        item.setExtensionName(extensionName);
        item.setPropertyName(propertyName);
        item.setType(type);
        item.setDocs(docs);
        item.setLocation(location);
        item.setSource(source);
        item.setPhase(phase);
        item.setDefaultValue(defaultValue);
        return item;
    }

}
