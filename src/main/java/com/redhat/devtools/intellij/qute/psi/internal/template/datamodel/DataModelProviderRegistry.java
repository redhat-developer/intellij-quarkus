/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.qute.psi.internal.template.datamodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.KeyedLazyInstanceEP;
import com.intellij.util.MergeQuery;
import com.intellij.util.Query;
import com.intellij.util.xmlb.annotations.Attribute;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.qute.psi.internal.AbstractQuteExtensionPointRegistry;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.IDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import com.redhat.qute.commons.QuteProjectScope;
import com.redhat.qute.commons.datamodel.resolvers.NamespaceResolverInfo;
import org.apache.commons.lang3.StringUtils;
import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelProject;
import com.redhat.qute.commons.datamodel.DataModelTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * Registry to handle instances of {@link IDataModelProvider}
 *
 * @author Angelo ZERR
 */
public class DataModelProviderRegistry extends AbstractQuteExtensionPointRegistry<IDataModelProvider, DataModelProviderRegistry.DataModelProviderBean> {

	private static final Logger LOGGER = Logger.getLogger(DataModelProviderRegistry.class.getName());

	private static final ExtensionPointName<DataModelProviderBean> DATA_MODEL_PROVIDERS_EXTENSION_POINT_ID = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.qute.dataModelProvider");

	private static final String NAMESPACES_ATTR = "namespaces";

	private static final String DESCRIPTION_ATTR = "description";

	private static final String URL_ATTR = "url";

	public static class DataModelProviderBean extends KeyedLazyInstanceEP<IDataModelProvider> {
		@Attribute(NAMESPACES_ATTR)
		public String namespaces;

		@Attribute(DESCRIPTION_ATTR)
		public String description;

		@Attribute(URL_ATTR)
		public String url;
	}


	private static final DataModelProviderRegistry INSTANCE = new DataModelProviderRegistry();

	private DataModelProviderRegistry() {
		super();
	}

	public static DataModelProviderRegistry getInstance() {
		return INSTANCE;
	}

	@Override
	public ExtensionPointName<DataModelProviderBean> getProviderExtensionId() {
		return DATA_MODEL_PROVIDERS_EXTENSION_POINT_ID;
	}

	@Override
	protected IDataModelProvider createInstance(DataModelProviderBean ce) {
		IDataModelProvider provider = super.createInstance(ce);
		String namespaces = ce.namespaces;
		if (StringUtils.isNotEmpty(namespaces)) {
			String description = ce.description;
			String url = ce.url;
			NamespaceResolverInfo info = new NamespaceResolverInfo();
			info.setNamespaces(Arrays.asList(namespaces.split(",")));
			info.setDescription(description);
			info.setUrl(url);
			provider.setNamespaceResolverInfo(info);
		}
		return provider;
	}

	/**
	 * Returns the data model project for the given java project.
	 * 
	 * @param javaProject the java project.
	 * @param scopes      the scopes used to scan Java classes.
	 * @param monitor     the progress monitor.
	 * @return the data model project for the given java project.
	 */
	public DataModelProject<DataModelTemplate<DataModelParameter>> getDataModelProject(Module javaProject,
																					   List<QuteProjectScope> scopes,
																					   IPsiUtils utils,
																					   ProgressIndicator monitor) {
		DataModelProject<DataModelTemplate<DataModelParameter>> project = new DataModelProject<DataModelTemplate<DataModelParameter>>();
		project.setTemplates(new ArrayList<>());
		project.setNamespaceResolverInfos(new HashMap<>());
		project.setValueResolvers(new ArrayList<>());
		collectDataModel(project, javaProject, scopes, utils, monitor);
		return project;
	}

	private void collectDataModel(DataModelProject<DataModelTemplate<DataModelParameter>> project,
			Module javaProject, List<QuteProjectScope> scopes, IPsiUtils utils, ProgressIndicator monitor) {
		long startTime = System.currentTimeMillis();
		if (LOGGER.isLoggable(Level.INFO)) {
			LOGGER.info("Start collecting Qute data model for '" + PsiQuteProjectUtils.getProjectURI(javaProject)
					+ "' project.");
		}

		String text = monitor.getText();
		monitor.setText("Scanning data model for '" + javaProject.getProject().getName() + "' project in '" + scopes.stream() //
				.map(QuteProjectScope::name) //
				.collect(Collectors.joining("+")) //
				+ "'"
		);
		ProgressIndicator mainMonitor = monitor;
		try {
			boolean excludeTestCode = true;

			// scan Java classes from the search classpath
			scanJavaClasses(javaProject, excludeTestCode, scopes, project, utils, mainMonitor);
			if (mainMonitor.isCanceled()) {
				throw new ProcessCanceledException();
			}
		} finally {
			if (LOGGER.isLoggable(Level.INFO)) {
				LOGGER.info("End collecting Qute data model for '" + PsiQuteProjectUtils.getProjectURI(javaProject)
						+ "' project in " + (System.currentTimeMillis() - startTime) + "ms.");
			}
			monitor.setText(text);
		}
	}

	private void scanJavaClasses(Module javaProject, boolean excludeTestCode, List<QuteProjectScope> scopes,
			DataModelProject<DataModelTemplate<DataModelParameter>> project, IPsiUtils utils,
								 ProgressIndicator mainMonitor) {
		// Create JDT Java search pattern, engine and scope
		String text = mainMonitor.getText();
		mainMonitor.setText("Scanning Java classes");
		ProgressIndicator subMonitor = mainMonitor;
		try {
			//subMonitor.split(5); // give feedback to the user that something is happening

			SearchContext context = new SearchContext(javaProject, project, utils, scopes);
			Query<?> pattern = createSearchPattern(context);
			if (pattern != null) {
				SearchScope scope = createSearchScope(javaProject, scopes, excludeTestCode, subMonitor);
				// Execute the search
				try {
					beginSearch(context, subMonitor);
					pattern.forEach((Consumer<Object>) psiMember -> collectDataModel(psiMember, context, mainMonitor));
				} finally {
					endSearch(context, subMonitor);
				}
			}
		} finally {
			mainMonitor.setText(text);
		}
	}

	private void beginSearch(SearchContext context, ProgressIndicator monitor) {
		for (IDataModelProvider provider : getProviders()) {
			provider.beginSearch(context, monitor);
		}
	}

	private void endSearch(SearchContext context, ProgressIndicator monitor) {
		for (IDataModelProvider provider : getProviders()) {
			provider.endSearch(context, monitor);
		}
	}

	private @Nullable Query<? extends Object> createSearchPattern(SearchContext context) {
		Query<? extends Object> leftPattern = null;
		for (IDataModelProvider provider : getProviders()) {
			if (leftPattern == null) {
				leftPattern = provider.createSearchPattern(context);
			} else {
				Query<? extends Object> rightPattern = provider.createSearchPattern(context);
				if (rightPattern != null) {
					leftPattern = new MergeQuery<>(leftPattern, rightPattern);
				}
			}
		}
		return leftPattern;
	}

	private void collectDataModel(Object match, SearchContext context, ProgressIndicator monitor) {
		for (IDataModelProvider provider : getProviders()) {
			try {
				provider.collectDataModel(match, context, monitor);
			} catch (ProcessCanceledException e) {
				//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
				//TODO delete block when minimum required version is 2024.2
				throw e;
			} catch (IndexNotReadyException | CancellationException e) {
				throw e;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING,
						"Error while collecting data model with the provider '" + provider.getClass().getName() + "'.",
						e);
			}
		}
	}

	private SearchScope createSearchScope(Module project, List<QuteProjectScope> scopes,
										  boolean excludeTestCode, ProgressIndicator monitor) {
		SearchScope searchScope = GlobalSearchScope.EMPTY_SCOPE;

		for (QuteProjectScope scope : scopes) {
			switch (scope) {
			case sources:
				searchScope = searchScope.union(project.getModuleScope(!excludeTestCode));
				break;
			case dependencies:
				searchScope = searchScope.union(project.getModuleWithLibrariesScope());
				break;
			}
		}
		// Standard Java Search in the project.
		// The search scope is used to search in src, jars
		return searchScope;
	}
}
