/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.module;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.search.internal.config.java.MicroProfileConfigHoverParticipant;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.Assert;

import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.metadata.ItemHint;
import org.eclipse.lsp4mp.commons.metadata.ItemHint.ValueHint;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;

/**
 * MicroProfile assert for JUnit tests.
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/internal/core/MicroProfileAssert.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/internal/core/MicroProfileAssert.java</a>
 *
 */
public class MicroProfileAssert {

	// ------------------------- Assert properties

	/**
	 * Assert MicroProfile properties.
	 * 
	 * @param info     the MicroProfile project information
	 * @param expected the expected MicroProfile properties.
	 */
	public static void assertProperties(MicroProfileProjectInfo info, ItemMetadata... expected) {
		assertProperties(info, null, expected);
	}

	/**
	 * Assert MicroProfile properties.
	 * 
	 * @param info          the MicroProfile project information
	 * @param expectedCount MicroProfile properties expected count.
	 * @param expected      the expected MicroProfile properties.
	 */
	public static void assertProperties(MicroProfileProjectInfo info, Integer expectedCount, ItemMetadata... expected) {
		if (expectedCount != null) {
			Assert.assertEquals(expectedCount.intValue(), info.getProperties().size());
		}
		for (ItemMetadata item : expected) {
			assertProperty(info, item);
		}
	}

	/**
	 * Assert MicroProfile metadata property
	 * 
	 * @param info     the MicroProfile project information
	 * @param expected the MicroProfile property.
	 */
	private static void assertProperty(MicroProfileProjectInfo info, ItemMetadata expected) {
		List<ItemMetadata> matches = info.getProperties().stream().filter(completion -> {
			return expected.getName().equals(completion.getName());
		}).collect(Collectors.toList());

		Assert.assertEquals(
				expected.getName() + " should only exist once: Actual: "
						+ info.getProperties().stream().map(c -> c.getName()).collect(Collectors.joining(",")),
				1, matches.size());

		ItemMetadata actual = matches.get(0);
		Assert.assertEquals("Test 'extension name' for '" + expected.getName() + "'", expected.getExtensionName(),
				actual.getExtensionName());
		Assert.assertEquals("Test 'type' for '" + expected.getName() + "'", expected.getType(), actual.getType());
		Assert.assertEquals("Test 'description' for '" + expected.getName() + "'", expected.getDescription(),
				actual.getDescription());
		Assert.assertEquals("Test 'binary' for '" + expected.getName() + "'", expected.isBinary(), actual.isBinary());
		Assert.assertEquals("Test 'source type' for '" + expected.getName() + "'", expected.getSourceType(),
				actual.getSourceType());
		Assert.assertEquals("Test 'source field' for '" + expected.getName() + "'", expected.getSourceField(),
				actual.getSourceField());
		Assert.assertEquals("Test 'source method' for '" + expected.getName() + "'", expected.getSourceMethod(),
				actual.getSourceMethod());
		Assert.assertEquals("Test 'phase' for '" + expected.getName() + "'", expected.getPhase(), actual.getPhase());
		Assert.assertEquals("Test 'default value' for '" + expected.getName() + "'", expected.getDefaultValue(),
				actual.getDefaultValue());
	}

	/**
	 * Returns an instance of MicroProfile property.
	 * 
	 * @param extensionName Quarkus extension name
	 * @param name          the property name
	 * @param type          the property class type
	 * @param description   the Javadoc
	 * @param binary        true if it comes from a binary field/method and false
	 *                      otherwise.
	 * @param sourceType    the source type (class or interface)
	 * @param sourceField   the source field name and null otherwise
	 * @param sourceMethod  the source method signature and null otherwise
	 * @param phase         the ConfigPhase.
	 * @param defaultValue  the default value
	 * @return
	 */
	public static ItemMetadata p(String extensionName, String name, String type, String description, boolean binary,
			String sourceType, String sourceField, String sourceMethod, int phase, String defaultValue) {
		ItemMetadata item = new ItemMetadata();
		item.setExtensionName(extensionName);
		item.setName(name);
		item.setType(type);
		item.setDescription(description);
		item.setSource(!binary);
		item.setSourceType(sourceType);
		item.setSourceMethod(sourceMethod);
		item.setSourceField(sourceField);
		item.setPhase(phase);
		item.setDefaultValue(defaultValue);
		return item;
	}

	/**
	 * Assert duplicate properties from the given the MicroProfile project
	 * information
	 * 
	 * @param info the MicroProfile project information
	 */
	public static void assertPropertiesDuplicate(MicroProfileProjectInfo info) {
		Map<String, Long> propertiesCount = info.getProperties().stream()
				.collect(Collectors.groupingBy(ItemMetadata::getName, Collectors.counting()));
		List<Entry<String, Long>> result = propertiesCount.entrySet().stream().filter(entry -> entry.getValue() > 1)
				.collect(Collectors.toList());
		Assert.assertEquals(
				result.stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(",")),
				0, result.size());
	}

	// ------------------------- Assert hints

	/**
	 * Assert MicroProfile hints.
	 * 
	 * @param info     the MicroProfile project information
	 * @param expected the expected MicroProfile hints.
	 */
	public static void assertHints(MicroProfileProjectInfo info, ItemHint... expected) {
		assertHints(info, null, expected);
	}

	/**
	 * Assert MicroProfile hints.
	 * 
	 * @param info          the MicroProfile project information
	 * @param expectedCount MicroProfile hints expected count.
	 * @param expected      the expected MicroProfile hints.
	 */
	public static void assertHints(MicroProfileProjectInfo info, Integer expectedCount, ItemHint... expected) {
		if (expectedCount != null) {
			Assert.assertEquals(expectedCount.intValue(), info.getHints().size());
		}
		for (ItemHint item : expected) {
			assertHint(info, item);
		}
	}

	/**
	 * Assert MicroProfile metadata hint
	 * 
	 * @param info     the MicroProfile project information
	 * @param expected the MicroProfile hint.
	 */
	private static void assertHint(MicroProfileProjectInfo info, ItemHint expected) {
		List<ItemHint> matches = info.getHints().stream().filter(completion -> {
			return expected.getName().equals(completion.getName());
		}).collect(Collectors.toList());

		Assert.assertEquals(
				expected.getName() + " should only exist once: Actual: "
						+ info.getHints().stream().map(c -> c.getName()).collect(Collectors.joining(",")),
				1, matches.size());

		ItemHint actual = matches.get(0);
		Assert.assertEquals("Test 'description' for '" + expected.getName() + "'", expected.getDescription(),
				actual.getDescription());
	}

	/**
	 * Returns an instance of MicroProfile {@link ItemHint}.
	 * 
	 * @param name        the property name
	 * @param description the Javadoc
	 * @param binary      true if it comes from a binary field/method and false
	 *                    otherwise.
	 * @param sourceType  the source type (class or interface)
	 * @param values      the hint values
	 * @return an instance of MicroProfile {@link ItemHint}.
	 */
	public static ItemHint h(String name, String description, boolean binary, String sourceType, ValueHint... values) {
		ItemHint item = new ItemHint();
		item.setName(name);
		if (!binary) {
			item.setSource(Boolean.TRUE);
		}
		item.setDescription(description);
		item.setSourceType(sourceType);
		if (values != null) {
			item.setValues(Arrays.asList(values));
		}
		return item;
	}

	/**
	 * Returns an instance of MicroProfile {@link ValueHint}.
	 * 
	 * @param value
	 * @param description
	 * @param sourceType
	 * @return an instance of MicroProfile {@link ValueHint}.
	 */
	public static ValueHint vh(String value, String description, String sourceType) {
		ValueHint vh = new ValueHint();
		vh.setValue(value);
		vh.setDescription(description);
		vh.setSourceType(sourceType);
		return vh;
	}

	/**
	 * Assert duplicate hints from the given the MicroProfile project information
	 * 
	 * @param info the MicroProfile project information
	 */
	public static void assertHintsDuplicate(MicroProfileProjectInfo info) {
		Map<String, Long> hintsCount = info.getHints().stream()
				.collect(Collectors.groupingBy(ItemHint::getName, Collectors.counting()));
		List<Entry<String, Long>> result = hintsCount.entrySet().stream().filter(entry -> entry.getValue() > 1)
				.collect(Collectors.toList());
		Assert.assertEquals(
				result.stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(",")),
				0, result.size());
	}

    public static void saveFile(String name, String content, Module javaProject) throws IOException {
        String modulePath = ModuleUtilCore.getModuleDirPath(javaProject);
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByPath(modulePath + "/src/main/resources/" + name);
        Application application = ApplicationManager.getApplication();
        application.invokeAndWait(() -> application.runWriteAction(() -> {
            try {
                file.setBinaryContent(content.getBytes(file.getCharset()));
            } catch (IOException e) {}
        }));
    }

	public static void assertHover(String expectedKey, String expectedValue, int expectedLine, int expectedStartOffset,
								   int expectedEndOffset, Hover actualInfo) {
		Assert.assertNotNull(actualInfo);

		Position expectedStart = new Position(expectedLine, expectedStartOffset);
		Position expectedEnd = new Position(expectedLine, expectedEndOffset);
		Range expectedRange = new Range(expectedStart, expectedEnd);

		MarkupContent expectedContent = MicroProfileConfigHoverParticipant.getDocumentation(expectedKey, expectedValue,
				DocumentFormat.Markdown, true);

		Assert.assertEquals(expectedContent, actualInfo.getContents().getRight());
		Assert.assertEquals(expectedRange, actualInfo.getRange());
	}
}
