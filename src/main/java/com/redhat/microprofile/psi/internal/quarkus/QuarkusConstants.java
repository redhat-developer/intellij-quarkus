/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus;

/**
 * Quarkus constants.
 * 
 * @author Angelo ZERR
 *
 */
public class QuarkusConstants {

	/**
	 * Quarkus prefix used in the Quarkus property.
	 */
	public static final String QUARKUS_PREFIX = "quarkus";

	/**
	 * The Quarkus @ConfigRoot annotation
	 */
	public static final String CONFIG_ROOT_ANNOTATION = "io.quarkus.runtime.annotations.ConfigRoot";

	public static final String CONFIG_ROOT_ANNOTATION_NAME = "name";

	public static final String CONFIG_ROOT_ANNOTATION_PHASE = "phase";

	/**
	 * The Quarkus @ConfigGroup annotation
	 */
	public static final String CONFIG_GROUP_ANNOTATION = "io.quarkus.runtime.annotations.ConfigGroup";

	/**
	 * The Quarkus @ConfigItem annotation
	 */
	public static final String CONFIG_ITEM_ANNOTATION = "io.quarkus.runtime.annotations.ConfigItem";

	public static final String CONFIG_ITEM_ANNOTATION_DEFAULT_VALUE = "defaultValue";

	public static final String CONFIG_ITEM_ANNOTATION_NAME = "name";

	/**
	 * The Quarkus @Scheduled annotation
	 */
	public static final String SCHEDULED_ANNOTATION = "io.quarkus.scheduler.Scheduled";
	public static final String SCHEDULED_ANNOTATION_CRON = "cron";
	public static final String SCHEDULED_ANNOTATION_EVERY = "every";
	public static final String SCHEDULED_ANNOTATION_IDENTITY = "identity";
	public static final String SCHEDULED_ANNOTATION_DELAY = "delay";
	public static final String SCHEDULED_ANNOTATION_DELAY_UNIT = "delayUnit";
	public static final String SCHEDULED_ANNOTATION_DELAYED = "delayed";

	public static final String[] SCHEDULED_SUPPORTED_PARTICIPANT_MEMBERS = {
			SCHEDULED_ANNOTATION_CRON, SCHEDULED_ANNOTATION_EVERY, SCHEDULED_ANNOTATION_DELAY,
			SCHEDULED_ANNOTATION_DELAYED, SCHEDULED_ANNOTATION_DELAY_UNIT
	};
	public static final String SCHEDULED_ANNOTATION_CONCURRENT_EXECUTION = "concurrentExecution";
	public static final String SCHEDULED_ANNOTATION_SKIP_EXECUTION_IF = "skipExecutionIf";

	/**
	 * The Quarkus @CacheResult annotation
	 */
	public static final String CACHE_RESULT_ANNOTATION = "io.quarkus.cache.CacheResult";

	public static final String CACHE_RESULT_ANNOTATION_CACHE_NAME = "cacheName";

	/**
	 * The Quarkus @ConfigMapping annotation
	 */
	public static final String CONFIG_MAPPING_ANNOTATION = "io.smallrye.config.ConfigMapping";

	public static final String CONFIG_MAPPING_ANNOTATION_PREFIX = "prefix";

	public static final String CONFIG_MAPPING_ANNOTATION_NAMING_STRATEGY = "namingStrategy";

	public static final String WITH_NAME_ANNOTATION = "io.smallrye.config.WithName";

	public static final String WITH_NAME_ANNOTATION_VALUE = "value";

	public static final String WITH_PARENT_NAME_ANNOTATION = "io.smallrye.config.WithParentName";

	public static final String WITH_DEFAULT_ANNOTATION = "io.smallrye.config.WithDefault";

	public static final String WITH_DEFAULT_ANNOTATION_VALUE = "value";

	/**
	 * The Quarkus @ConfigProperties annotation
	 */
	public static final String CONFIG_PROPERTIES_ANNOTATION = "io.quarkus.arc.config.ConfigProperties";

	public static final String CONFIG_PROPERTIES_ANNOTATION_PREFIX = "prefix";

	public static final String QUARKUS_ARC_CONFIG_PROPERTIES_DEFAULT_NAMING_STRATEGY = "quarkus.arc.config-properties-default-naming-strategy";

	/**
	 * Quarkus properties file embedded in the Quarkus JAR.
	 */
	public static final String QUARKUS_JAVADOC_PROPERTIES_FILE = "quarkus-javadoc.properties";
	public static final String QUARKUS_EXTENSION_PROPERTIES_FILE = "quarkus-extension.properties";

	public static final String DEPLOYMENT_ARTIFACT_PROPERTY = "deployment-artifact";

	public static final String QUARKUS_RUNTIME_CLASS_NAME = "io.quarkus.runtime.LaunchMode";

	private QuarkusConstants() {
	}
}