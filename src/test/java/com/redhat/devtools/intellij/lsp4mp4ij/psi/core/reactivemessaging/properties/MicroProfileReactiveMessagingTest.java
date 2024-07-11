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
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.LSP4MPMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileMavenProjectName;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.reactivemessaging.MicroProfileReactiveMessagingConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.reactivemessaging.java.MicroProfileReactiveMessagingErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.*;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.*;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaDiagnostics;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.d;

/**
 * Test collection of MicroProfile properties for MicroProfile Reactive
 * Messaging annotations
 *
 * @author Angelo ZERR
 */
public class MicroProfileReactiveMessagingTest extends LSP4MPMavenModuleImportingTestCase {

    @Test
    public void testMicroprofileReactiveMessagingPropertiesTest() throws Exception {

        Module module = loadMavenProject(MicroProfileMavenProjectName.microprofile_reactive_messaging);
        MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.PlainText, new EmptyProgressIndicator());

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
                        true, "io.smallrye.reactive.messaging.kafka.KafkaConnector", null, null, 0, "localhost:9092"),

                // mp.messaging.outgoing.quotes.connector
                p(null, "mp.messaging.outgoing.quotes.connector",
                        "org.eclipse.microprofile.reactive.messaging.spi.Connector", null, false,
                        "org.acme.kafka.QuoteResource", "quotes", null, 0, null)
        );

        assertPropertiesDuplicate(infoFromClasspath);

        assertHints(infoFromClasspath, h("${mp.messaging.connector.binary}", null, true, null, //
                vh("smallrye-kafka", null, "io.smallrye.reactive.messaging.kafka.KafkaConnector")) //
        );

        assertHintsDuplicate(infoFromClasspath);
    }

    @Test
    public void testBlankAnnotation() throws Exception {
        Module javaProject = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-reactive-messaging"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(javaProject) + "/src/main/java/org/acme/kafka/PriceConverter.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        diagnosticsParams.setUris(Arrays
                .asList(uri));
        diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d1 = d(24, 14, 16,
                "The name of the consumed channel must not be blank.",
                DiagnosticSeverity.Error,
                MicroProfileReactiveMessagingConstants.MICRO_PROFILE_REACTIVE_MESSAGING_DIAGNOSTIC_SOURCE,
                MicroProfileReactiveMessagingErrorCode.BLANK_CHANNEL_NAME);
        Diagnostic d2 = d(25, 20, 22,
                "The name of the consumed channel must not be blank.",
                DiagnosticSeverity.Error,
                MicroProfileReactiveMessagingConstants.MICRO_PROFILE_REACTIVE_MESSAGING_DIAGNOSTIC_SOURCE,
                MicroProfileReactiveMessagingErrorCode.BLANK_CHANNEL_NAME);
        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);
    }

}
