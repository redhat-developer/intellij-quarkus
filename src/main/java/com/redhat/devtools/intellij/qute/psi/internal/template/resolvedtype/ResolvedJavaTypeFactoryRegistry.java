/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.qute.psi.internal.template.resolvedtype;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.util.KeyedLazyInstanceEP;
import com.intellij.util.xmlb.annotations.Attribute;
import com.redhat.devtools.intellij.qute.psi.internal.AbstractQuteExtensionPointRegistry;

import com.redhat.qute.commons.ResolvedJavaTypeInfo;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverKind;

/**
 * Registry to handle instances of {@link IResolvedJavaTypeFactory}
 *
 * @author Angelo ZERR
 */
public class ResolvedJavaTypeFactoryRegistry extends AbstractQuteExtensionPointRegistry<IResolvedJavaTypeFactory, ResolvedJavaTypeFactoryRegistry.ResolvedJavaTypeFactoryBean> {

	public static class ResolvedJavaTypeFactoryBean extends KeyedLazyInstanceEP<IResolvedJavaTypeFactory> {

	}

	private static final ExtensionPointName<ResolvedJavaTypeFactoryRegistry.ResolvedJavaTypeFactoryBean> RESOLVED_JAVA_TYPE_FACTORIES_EXTENSION_POINT_ID = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.qute.resolvedJavaTypeFactories");

	private static final ResolvedJavaTypeFactoryRegistry INSTANCE = new ResolvedJavaTypeFactoryRegistry();

	private ResolvedJavaTypeFactoryRegistry() {
		super();
	}

	public static ResolvedJavaTypeFactoryRegistry getInstance() {
		return INSTANCE;
	}

	@Override
	public ExtensionPointName<ResolvedJavaTypeFactoryBean> getProviderExtensionId() {
		return RESOLVED_JAVA_TYPE_FACTORIES_EXTENSION_POINT_ID;
	}

	public ResolvedJavaTypeInfo create(PsiClass type, ValueResolverKind kind, Module javaProject) {
		return getFactory(kind).create(type, javaProject);
	}

	private IResolvedJavaTypeFactory getFactory(ValueResolverKind kind) {
		for (IResolvedJavaTypeFactory factory : getProviders()) {
			if (factory.isAdaptedFor(kind)) {
				return factory;
			}
		}
		return DefaultResolvedJavaTypeFactory.getInstance();
	}
}