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
package com.redhat.devtools.intellij.quarkus.gradle;

import com.redhat.devtools.intellij.quarkus.search.PSIQuarkusManager;
import com.redhat.quarkus.commons.ExtendedConfigDescriptionBuildItem;
import com.redhat.quarkus.commons.QuarkusPropertiesScope;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static com.redhat.devtools.intellij.quarkus.module.QuarkusAssert.assertProperties;
import static com.redhat.devtools.intellij.quarkus.module.QuarkusAssert.p;
import static com.redhat.quarkus.commons.ExtendedConfigDescriptionBuildItem.CONFIG_PHASE_BUILD_TIME;

public class GradlePSIQuarkusManagerConfigRootHibernateRestEasyTest extends GradleTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        FileUtils.copyDirectory(new File("projects/gradle/hibernate-orm-resteasy"), new File(getProjectPath()));
        importProject();
    }

    @Test
    public void testHibernateOrmResteasy() throws Exception {
        List<ExtendedConfigDescriptionBuildItem> items = PSIQuarkusManager.INSTANCE.getConfigItems(getModule("hibernate-orm-resteasy.main"), QuarkusPropertiesScope.classpath, false);
        File f = getDependency(getProjectPath(), "io.quarkus", "quarkus-hibernate-orm-deployment", "1.0.1.Final");
        assertNotNull("Test existing of quarkus-hibernate-orm-deployment*.jar", f);
        assertProperties(items,

                // io.quarkus.hibernate.orm.deployment.HibernateOrmConfig
                p("quarkus-hibernate-orm", "quarkus.hibernate-orm.dialect", "java.util.Optional<java.lang.String>",
                        DOC, f.getAbsolutePath(),
                        "io.quarkus.hibernate.orm.deployment.HibernateOrmConfig#dialect", CONFIG_PHASE_BUILD_TIME,
                        null));

    }

}
