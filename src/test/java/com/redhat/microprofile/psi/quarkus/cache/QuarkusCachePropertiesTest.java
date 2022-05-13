/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.quarkus.cache;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertHints;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertHintsDuplicate;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertPropertiesDuplicate;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.h;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.p;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.vh;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenProjectName;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Test;

import java.io.File;

/**
 * Test collection of Quarkus properties from @CacheResult
 */
public class QuarkusCachePropertiesTest extends MavenModuleImportingTestCase {

	@Test
	public void testCacheQuickstartFromClasspath() throws Exception {

		Module javaProject = createMavenModule(QuarkusMavenProjectName.cache_quickstart, new File("projects/quarkus/projects/maven/" + QuarkusMavenProjectName.cache_quickstart));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);
		MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(
				javaProject, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC,
				PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.Markdown);



		assertProperties(infoFromClasspath,
				// WeatherForecastService
				// @CacheResult(cacheName = "weather-cache")
				// public String getDailyForecast(LocalDate date, String city) {
				p("quarkus-cache", "quarkus.cache.enabled", "boolean", "Whether or not the cache extension is enabled.",
						true, "io.quarkus.cache.deployment.CacheConfig", "enabled", null, 1, "true"),
				p("quarkus-cache", "quarkus.cache.caffeine.${quarkus.cache.name}.initial-capacity",
						"java.util.OptionalInt",
						"Minimum total size for the internal data structures. Providing a large enough estimate at construction time\navoids the need for expensive resizing operations later, but setting this value unnecessarily high wastes memory.",
						true, "io.quarkus.cache.deployment.CacheConfig$CaffeineConfig$CaffeineNamespaceConfig",
						"initialCapacity", null, 1, null));

		assertPropertiesDuplicate(infoFromClasspath);
		
		assertHints(infoFromClasspath, h("${quarkus.cache.name}", null, false, null, //
				vh("weather-cache", null, null)) //
		);

		assertHintsDuplicate(infoFromClasspath);

	}

}