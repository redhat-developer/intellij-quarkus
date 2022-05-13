/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
* 
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.hibernate.properties;

import com.redhat.microprofile.psi.internal.quarkus.providers.AbstractStaticQuarkusPropertiesProvider;

/**
 * Properties provider that provides static Quarkus Hibernate ORM properties
 * 
 * @author Angelo ZERR
 * 
 * @see https://github.com/eclipse/microprofile-health/blob/master/spec/src/main/asciidoc/protocol-wireformat.adoc
 *
 */
public class QuarkusHibernateORMProvider extends AbstractStaticQuarkusPropertiesProvider {

	public QuarkusHibernateORMProvider() {
		super("/static-properties/quarkus-hibernate-orm-metadata.json");
	}
}