/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.run;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import org.jdom.Element;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

/**
 * Quarkus run configuration test
 */
public class MavenRunConfigurationTest extends MavenModuleImportingTestCase {

	@Test
	public void testDefaultRunConfiguration() throws Exception {
		Module module = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
		RunnerAndConfigurationSettings settings = RunManager.getInstance(module.getProject()).createConfiguration(module.getName() + " (Maven)", QuarkusRunConfigurationType.class);
		QuarkusRunConfiguration configuration = (QuarkusRunConfiguration) settings.getConfiguration();
		configuration.setModule(module);
		Element element = new Element("configuration");
		configuration.writeExternal(element);
		QuarkusRunConfiguration configuration1 = (QuarkusRunConfiguration) RunManager.getInstance(module.getProject()).createConfiguration(module.getName() + " (Maven)1", QuarkusRunConfigurationType.class).getConfiguration();
		configuration1.readExternal(element);
		assertEquals(configuration, configuration1);
	}

	private void assertEquals(QuarkusRunConfiguration configuration, QuarkusRunConfiguration configuration1) {
		assertEquals(configuration.getProfile(), configuration1.getProfile());
		assertEquals(configuration.getEnv(), configuration1.getEnv());
	}

	@Test
	public void testRunConfigurationWithProfile() throws Exception {
		Module module = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
		RunnerAndConfigurationSettings settings = RunManager.getInstance(module.getProject()).createConfiguration(module.getName() + " (Maven)", QuarkusRunConfigurationType.class);
		QuarkusRunConfiguration configuration = (QuarkusRunConfiguration) settings.getConfiguration();
		configuration.setModule(module);
		configuration.setProfile("myprofile");
		Element element = new Element("configuration");
		configuration.writeExternal(element);
		QuarkusRunConfiguration configuration1 = (QuarkusRunConfiguration) RunManager.getInstance(module.getProject()).createConfiguration(module.getName() + " (Maven)1", QuarkusRunConfigurationType.class).getConfiguration();
		configuration1.readExternal(element);
		assertEquals(configuration, configuration1);
	}

	@Test
	public void testRunConfigurationWithEnv() throws Exception {
		Module module = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
		RunnerAndConfigurationSettings settings = RunManager.getInstance(module.getProject()).createConfiguration(module.getName() + " (Maven)", QuarkusRunConfigurationType.class);
		QuarkusRunConfiguration configuration = (QuarkusRunConfiguration) settings.getConfiguration();
		configuration.setModule(module);
		configuration.setEnv(Collections.singletonMap("mykey", "myvalue"));
		Element element = new Element("configuration");
		configuration.writeExternal(element);
		QuarkusRunConfiguration configuration1 = (QuarkusRunConfiguration) RunManager.getInstance(module.getProject()).createConfiguration(module.getName() + " (Maven)1", QuarkusRunConfigurationType.class).getConfiguration();
		configuration1.readExternal(element);
		assertEquals(configuration, configuration1);
	}
}
