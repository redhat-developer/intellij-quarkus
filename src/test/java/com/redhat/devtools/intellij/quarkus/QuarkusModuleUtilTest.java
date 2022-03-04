/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus;

import org.junit.Test;

import static com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil.APPLICATION_PROPERTIES;
import static com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil.APPLICATION_YAML;
import static com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil.MICROPROFILE_CONFIG_PROPERTIES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuarkusModuleUtilTest {
    @Test
    public void checkApplicationPropertiesMatches() {
        assertTrue(APPLICATION_PROPERTIES.matcher("application.properties").matches());
    }

    @Test
    public void checkApplicationPropertiesWithOnlyDashDoesntMatch() {
        assertFalse(APPLICATION_PROPERTIES.matcher("application-.properties").matches());
    }

    @Test
    public void checkApplicationPropertiesWithoutDashDoesntMatch() {
        assertFalse(APPLICATION_PROPERTIES.matcher("applicationdev.properties").matches());
    }

    @Test
    public void checkApplicationPropertiesWithProfileMatches() {
        assertTrue(APPLICATION_PROPERTIES.matcher("application-dev.properties").matches());
    }

    @Test
    public void checkApplicationYAMLMatches() {
        assertTrue(APPLICATION_YAML.matcher("application.yaml").matches());
    }

    @Test
    public void checkApplicationYAMLWithOnlyDashDoesntMatch() {
        assertFalse(APPLICATION_YAML.matcher("application-.yaml").matches());
    }

    @Test
    public void checkApplicationYAMLWithoutDashDoesntMatch() {
        assertFalse(APPLICATION_YAML.matcher("applicationdev.yaml").matches());
    }

    @Test
    public void checkApplicationYAMLWithProfileMatches() {
        assertTrue(APPLICATION_YAML.matcher("application-dev.yaml").matches());
    }

    @Test
    public void checkApplicationYMLMatches() {
        assertTrue(APPLICATION_YAML.matcher("application.yml").matches());
    }

    @Test
    public void checkApplicationYMLWithOnlyDashDoesntMatch() {
        assertFalse(APPLICATION_YAML.matcher("application-.yml").matches());
    }

    @Test
    public void checkApplicationYMLWithoutDashDoesntMatch() {
        assertFalse(APPLICATION_YAML.matcher("applicationdev.yml").matches());
    }

    @Test
    public void checkApplicationYMLWithProfileMatches() {
        assertTrue(APPLICATION_YAML.matcher("application-dev.yml").matches());
    }

    @Test
    public void checkMicroprofileConfigPropertiesMatches() {
        assertTrue(MICROPROFILE_CONFIG_PROPERTIES.matcher("microprofile-config.properties").matches());
    }

    @Test
    public void checkMicroprofileConfigPropertiesWithOnlyDashDoesntMatch() {
        assertFalse(MICROPROFILE_CONFIG_PROPERTIES.matcher("microprofile-config-.properties").matches());
    }

    @Test
    public void checkMicroprofileConfigPropertiesWithoutDashDoesntMatch() {
        assertFalse(MICROPROFILE_CONFIG_PROPERTIES.matcher("microprofile-configdev.properties").matches());
    }

    @Test
    public void checkMicroprofileConfigPropertiesWithProfileMatches() {
        assertTrue(MICROPROFILE_CONFIG_PROPERTIES.matcher("microprofile-config-dev.properties").matches());
    }

}
