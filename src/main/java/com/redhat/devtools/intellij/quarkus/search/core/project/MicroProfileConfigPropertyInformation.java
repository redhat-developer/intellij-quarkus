/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.quarkus.search.core.project;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a config value for a given property and profile, along with a
 * reference to where the value was assigned
 *
 * @author datho7561
 * @see <a href="https://github.com/eclipse/lsp4mp/blob/master/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/core/project/MicroProfileConfigPropertyInformation.java">https://github.com/eclipse/lsp4mp/blob/master/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/core/project/MicroProfileConfigPropertyInformation.java</a>
 */
public class MicroProfileConfigPropertyInformation {

	public static final String DEFAULT_PROFILE = "";

	private final String propertyNameWithProfile;
	private final String value;
	private final String configFileName;
	private String profile;
	private String property;

	/**
	 *
	 * @param propertyNameWithProfile the property and profile, in the format used
	 *                                by microprofile-config.properties
	 * @param value                   the value of the property for this profile
	 * @param configFileName          the name of the file where the value for this
	 *                                property was declared, or null if the value
	 *                                was not declared in a file
	 */
	public MicroProfileConfigPropertyInformation(String propertyNameWithProfile, String value, String configFileName) {
		this.propertyNameWithProfile = propertyNameWithProfile;
		this.value = value;
		this.configFileName = configFileName;
		this.profile = null;
		this.property = null;
	}

	/**
	 * Returns the profile for this property information
	 *
	 * @return the profile for this property information
	 */
	public String getProfile() {
		if (profile == null) {
			if (propertyNameWithProfile.charAt(0) == '%') {
				profile = propertyNameWithProfile.substring(1, propertyNameWithProfile.indexOf('.'));
			} else {
				profile = DEFAULT_PROFILE;
			}
		}
		return profile;
	}

	/**
	 * Returns the name of the property with the profile in the format used by
	 * microprofile-config.properties
	 *
	 * eg. <code>%dev.my.property</code> or <code>my.property</code>
	 *
	 * @return the name of the property with the profile in the format used by
	 *         microprofile-config.properties
	 */
	public String getPropertyNameWithProfile() {
		return this.propertyNameWithProfile;
	}

	/**
	 * Returns the name of the property without the profile
	 *
	 * @return the name of the property without the profile
	 */
	public String getPropertyNameWithoutProfile() {
		if (property == null) {
			property = getPropertyNameWithoutProfile(propertyNameWithProfile);
		}
		return property;
	}

	/**
	 * Returns the value of this property
	 *
	 * @return the value of this property
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * Returns the name of the configuration file where the value of this property
	 * is declared, or null if the value was not declared in a file
	 *
	 * @return the name of the configuration file where the value of this property
	 *         is declared, or null if the value was not declared in a file
	 */
	public String getConfigFileName() {
		return this.configFileName;
	}

	/**
	 * Returns the property name with any profile information removed
	 *
	 * eg. %dev.my.property -> my.property ; my.other.property -> my.other.property
	 *
	 * @param propertyNameWithProfile the property and profile in the format used my
	 *                                microprofile-config.properties
	 * @return the property name with any profile information removed
	 */
	public static String getPropertyNameWithoutProfile(String propertyNameWithProfile) {
		if (propertyNameWithProfile.charAt(0) == '%') {
			int firstPeriodIndex = propertyNameWithProfile.indexOf('.');
			return propertyNameWithProfile.substring(firstPeriodIndex + 1);
		} else {
			return propertyNameWithProfile;
		}
	}

	/**
	 * Returns a list of segments of the property name
	 *
	 * @param propertyNameWithProfile the property and profile in the format used in microprofile-config.properties
	 * @return
	 */
	public static List<String> getSegments(String propertyNameWithProfile) {
		return Arrays.asList(propertyNameWithProfile.split("\\."));
	}
}
