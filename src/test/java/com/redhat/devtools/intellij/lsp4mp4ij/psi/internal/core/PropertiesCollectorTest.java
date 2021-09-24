/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.IPropertiesCollector.MergingStrategy;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.PropertiesCollector;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.commons.metadata.ConfigurationMetadata;
import org.eclipse.lsp4mp.commons.metadata.ItemHint;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.commons.metadata.ValueHint;
import org.junit.Test;

/**
 * Test for {@link PropertiesCollector}.
 *
 * @author Angelo ZERR
 *
 */
public class PropertiesCollectorTest {

	// ------------ Test with properties merge

	@Test
	public void merge() {
		ConfigurationMetadata configuration = new ConfigurationMetadata();
		PropertiesCollector collector = new PropertiesCollector(configuration,
				MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES);

		ConfigurationMetadata toMerge = createToMerge();
		collector.merge(toMerge);

		assertEquals(2, configuration.getProperties().size());

		ConfigurationMetadata dupMerge = createDuplicateMerge();
		collector.merge(dupMerge); // implicitly using MergingStrategy.FORCE

		assertEquals(3, configuration.getProperties().size());
	}

	@Test
	public void mergeWithOnlySources() {
		ConfigurationMetadata configuration = new ConfigurationMetadata();
		PropertiesCollector collector = new PropertiesCollector(configuration,
				MicroProfilePropertiesScope.ONLY_SOURCES);

		ConfigurationMetadata toMerge = createToMerge();
		collector.merge(toMerge);

		assertEquals(1, configuration.getProperties().size());
	}

	@Test
	public void mergeWithReplace() {
		ConfigurationMetadata configuration = new ConfigurationMetadata();
		PropertiesCollector collector = new PropertiesCollector(configuration,
				MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES);

		ConfigurationMetadata toMerge = createToMerge();
		collector.merge(toMerge, MergingStrategy.REPLACE);

		assertEquals(2, configuration.getProperties().size());
		assertNull(configuration.getProperties().get(0).getDescription());

		ConfigurationMetadata dupMerge = createDuplicateMerge();
		collector.merge(dupMerge, MergingStrategy.REPLACE);

		assertEquals(2, configuration.getProperties().size());
		assertNotNull(configuration.getProperties().get(1).getDescription());
	}

	@Test
	public void mergeWithIgnore() {
		ConfigurationMetadata configuration = new ConfigurationMetadata();
		PropertiesCollector collector = new PropertiesCollector(configuration,
				MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES);

		ConfigurationMetadata toMerge = createToMerge();
		collector.merge(toMerge, MergingStrategy.IGNORE_IF_EXISTS);

		assertEquals(2, configuration.getProperties().size());
		assertNull(configuration.getProperties().get(0).getDescription());

		ConfigurationMetadata dupMerge = createDuplicateMerge();
		collector.merge(dupMerge, MergingStrategy.IGNORE_IF_EXISTS);

		assertEquals(2, configuration.getProperties().size());
		assertNull(configuration.getProperties().get(0).getDescription());
	}

	private static ConfigurationMetadata createToMerge() {
		ConfigurationMetadata toMerge = new ConfigurationMetadata();
		toMerge.setProperties(new ArrayList<>());
		// Add a binary metadata
		ItemMetadata binary = new ItemMetadata();
		binary.setName("binaryProperty");
		toMerge.getProperties().add(binary);

		// Add a source metadata
		ItemMetadata source = new ItemMetadata();
		source.setSource(true);
		toMerge.getProperties().add(source);
		return toMerge;
	}

	private static ConfigurationMetadata createDuplicateMerge() {
		ConfigurationMetadata dupMerge = new ConfigurationMetadata();
		dupMerge.setProperties(new ArrayList<>());

		// Add a second binary metadata
		ItemMetadata binary2 = new ItemMetadata();
		binary2.setName("binaryProperty");
		binary2.setDescription("binary property with description");
		dupMerge.getProperties().add(binary2);
		return dupMerge;
	}

	// ------------ Test with hint merge

	@Test
	public void mergeHintsWithIgnore() {

		ConfigurationMetadata configuration1 = new ConfigurationMetadata();
		PropertiesCollector collector = new PropertiesCollector(configuration1,
				MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES);

		ItemHint hint1 = collector.getItemHint("logging");
		hint1.getValues().add(vh("OFF", "OFF [1]"));
		hint1.getValues().add(vh("SEVERE", "SEVERE [1]"));
		hint1.getValues().add(vh("INFO", "INFO [1]"));

		ConfigurationMetadata configuration2 = new ConfigurationMetadata();
		ItemHint hint2 = new ItemHint();
		hint2.setName("logging");
		hint2.setValues(
				new ArrayList<>(Arrays.asList(vh("DEBUG", "DEBUG [2]"), vh("INFO", "INFO [2]"), vh("OFF", "OFF [2]"))));
		configuration2.setHints(new ArrayList<>(Arrays.asList(hint2)));

		collector.merge(configuration2, MergingStrategy.IGNORE_IF_EXISTS);

		assertEquals(1, configuration1.getHints().size());
		assertEquals(4, configuration1.getHints().get(0).getValues().size());
		assertEquals("OFF [1]", configuration1.getHints().get(0).getValues().get(0).getDescription());
		assertEquals("SEVERE [1]", configuration1.getHints().get(0).getValues().get(1).getDescription());
		assertEquals("INFO [1]", configuration1.getHints().get(0).getValues().get(2).getDescription());
		assertEquals("DEBUG [2]", configuration1.getHints().get(0).getValues().get(3).getDescription());
	}

	@Test
	public void mergeHintsWithReplace() {

		ConfigurationMetadata configuration1 = new ConfigurationMetadata();
		PropertiesCollector collector = new PropertiesCollector(configuration1,
				MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES);

		ItemHint hint1 = collector.getItemHint("logging");
		hint1.getValues().add(vh("OFF", "OFF [1]"));
		hint1.getValues().add(vh("SEVERE", "SEVERE [1]"));
		hint1.getValues().add(vh("INFO", "INFO [1]"));

		ConfigurationMetadata configuration2 = new ConfigurationMetadata();
		ItemHint hint2 = new ItemHint();
		hint2.setName("logging");
		hint2.setValues(
				new ArrayList<>(Arrays.asList(vh("DEBUG", "DEBUG [2]"), vh("INFO", "INFO [2]"), vh("OFF", "OFF [2]"))));
		configuration2.setHints(new ArrayList<>(Arrays.asList(hint2)));

		collector.merge(configuration2, MergingStrategy.REPLACE);

		assertEquals(1, configuration1.getHints().size());
		assertEquals(4, configuration1.getHints().get(0).getValues().size());
		assertEquals("SEVERE [1]", configuration1.getHints().get(0).getValues().get(0).getDescription());
		assertEquals("DEBUG [2]", configuration1.getHints().get(0).getValues().get(1).getDescription());
		assertEquals("INFO [2]", configuration1.getHints().get(0).getValues().get(2).getDescription());
		assertEquals("OFF [2]", configuration1.getHints().get(0).getValues().get(3).getDescription());
	}

	@Test
	public void mergeHintsWithForce() {

		ConfigurationMetadata configuration1 = new ConfigurationMetadata();
		PropertiesCollector collector = new PropertiesCollector(configuration1,
				MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES);

		ItemHint hint1 = collector.getItemHint("logging");
		hint1.getValues().add(vh("OFF", "OFF [1]"));
		hint1.getValues().add(vh("SEVERE", "SEVERE [1]"));
		hint1.getValues().add(vh("INFO", "INFO [1]"));

		ConfigurationMetadata configuration2 = new ConfigurationMetadata();
		ItemHint hint2 = new ItemHint();
		hint2.setName("logging");
		hint2.setValues(
				new ArrayList<>(Arrays.asList(vh("DEBUG", "DEBUG [2]"), vh("INFO", "INFO [2]"), vh("OFF", "OFF [2]"))));
		configuration2.setHints(new ArrayList<>(Arrays.asList(hint2)));

		collector.merge(configuration2, MergingStrategy.FORCE);

		assertEquals(1, configuration1.getHints().size());
		assertEquals(6, configuration1.getHints().get(0).getValues().size());
		assertEquals("OFF [1]", configuration1.getHints().get(0).getValues().get(0).getDescription());
		assertEquals("SEVERE [1]", configuration1.getHints().get(0).getValues().get(1).getDescription());
		assertEquals("INFO [1]", configuration1.getHints().get(0).getValues().get(2).getDescription());
		assertEquals("DEBUG [2]", configuration1.getHints().get(0).getValues().get(3).getDescription());
		assertEquals("INFO [2]", configuration1.getHints().get(0).getValues().get(4).getDescription());
		assertEquals("OFF [2]", configuration1.getHints().get(0).getValues().get(5).getDescription());
	}

	private static ValueHint vh(String value, String description) {
		ValueHint debug = new ValueHint();
		debug.setValue(value);
		debug.setDescription(description);
		return debug;
	}
}
