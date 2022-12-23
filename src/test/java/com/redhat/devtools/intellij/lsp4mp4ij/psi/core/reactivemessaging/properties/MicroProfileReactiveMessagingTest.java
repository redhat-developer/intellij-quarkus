/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.reactivemessaging.properties;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertHints;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertHintsDuplicate;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertPropertiesDuplicate;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.h;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.p;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.vh;

/**
 * Test collection of MicroProfile properties for MicroProfile Reactive
 * Messaging annotations
 *
 * @author Angelo ZERR
 *
 */
public class MicroProfileReactiveMessagingTest extends MavenModuleImportingTestCase {

	@Test
	public void testMicroprofileReactiveMessagingPropertiesTest() throws Exception {

		Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-reactive-messaging"));
		MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.PlainText);

		assertProperties(infoFromClasspath,

				// mp.messaging.incoming.
				p(null, "mp.messaging.incoming.prices.connector",
						"org.eclipse.microprofile.reactive.messaging.spi.Connector", null, false,
						"org.acme.kafka.PriceConverter", null, "process(I)D", 0, null),

				// mp.messaging.outgoing
				p(null, "mp.messaging.outgoing.my-data-stream.connector",
						"org.eclipse.microprofile.reactive.messaging.spi.Connector", null, false,
						"org.acme.kafka.PriceConverter", null, "process(I)D", 0, null),

				// mp.messaging.incoming.${connector-name}
				p(null, "mp.messaging.incoming.${smallrye-kafka}.topic", "java.lang.String",
						"The consumed / populated Kafka topic. If not set, the channel name is used", true,
						"io.smallrye.reactive.messaging.kafka.KafkaConnector", null, null, 0, null),

				// mp.messaging.outgoing.${connector-name}
				p(null, "mp.messaging.outgoing.${smallrye-kafka}.topic", "java.lang.String",
						"The consumed / populated Kafka topic. If not set, the channel name is used", true,
						"io.smallrye.reactive.messaging.kafka.KafkaConnector", null, null, 0, null),

				// mp.messaging.incoming.${connector-name}
				p(null, "mp.messaging.incoming.${smallrye-kafka}.bootstrap.servers", "java.lang.String",
						"A comma-separated list of host:port to use for establishing the initial connection to the Kafka cluster.",
						true, "io.smallrye.reactive.messaging.kafka.KafkaConnector", null, null, 0, "localhost:9092") //
		);

		assertPropertiesDuplicate(infoFromClasspath);

		assertHints(infoFromClasspath, h("${mp.messaging.connector.binary}", null, true, null, //
				vh("smallrye-kafka", null, "io.smallrye.reactive.messaging.kafka.KafkaConnector")) //
		);

		assertHintsDuplicate(infoFromClasspath);
	}
}
