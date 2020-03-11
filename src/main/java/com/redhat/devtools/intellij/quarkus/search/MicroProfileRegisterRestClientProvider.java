/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;

import com.redhat.microprofile.commons.metadata.ItemHint;
import com.redhat.microprofile.commons.metadata.ItemHint.ValueHint;

/**
 * Properties provider to collect MicroProfile REST client properties from the
 * Java classes annotated with
 * "org.eclipse.microprofile.rest.client.inject.RegisterRestClient" annotation.
 * 
 * This provider generates:
 * 
 * <ul>
 * <li>dynamic properties like ${mp.register.rest.client.class}/mp-rest/url, etc
 * </li>
 * <li>hints whith classes annotated with @RegisterRestClient</li>
 * </ul>
 * 
 * Here a JSON sample:
 * 
 * <code>
 * {
	"properties": [
		{
			"type": "java.lang.String",
			"required": true,
			"phase": 0,
			"name": "${mp.register.rest.client.class}/mp-rest/url",
			"description": "The base URL to use for this service, the equivalent of the `baseUrl` method.\r\nThis property is considered required, however implementations may have other ways to define these URLs.",
			"source": true
		},
		{
			"type": "java.lang.String",
			"required": true,
			"phase": 0,
			"name": "${mp.register.rest.client.class}/mp-rest/scope",
			"description": "The fully qualified classname to a CDI scope to use for injection, defaults to `javax.enterprise.context.Dependent` as mentioned above.",
			"source": true
		},
		{
			"type": "java.lang.String",
			"required": true,
			"phase": 0,
			"name": "${mp.register.rest.client.class}/mp-rest/providers",
			"description": "A comma separated list of fully-qualified provider classnames to include in the client, the equivalent of the `register` method or the `@RegisterProvider` annotation.",
			"source": true
		}
	],
	"hints": [
		{
			"values": [
				{
					"value": "configKey",
					"sourceType": "org.acme.restclient.CountiesServiceWithConfigKey"
				},
				{
					"value": "org.acme.restclient.CountriesService",
					"sourceType": "org.acme.restclient.CountriesService"
				}
			],
			"name": "${mp.register.rest.client.class}",
			"source": true
		}
	]
}
 * </code>
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/providers/MicroProfileRegisterRestClientProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/providers/MicroProfileRegisterRestClientProvider.java</a>
 *
 */
public class MicroProfileRegisterRestClientProvider extends AbstractAnnotationTypeReferencePropertiesProvider {

	private static final String REGISTER_REST_CLIENT_ANNOTATION = "org.eclipse.microprofile.rest.client.inject.RegisterRestClient";
	private static final String REGISTER_REST_CLIENT_ANNOTATION_CONFIG_KEY = "configKey";

	private static final String[] ANNOTATION_NAMES = { REGISTER_REST_CLIENT_ANNOTATION };

	private static final String MP_REST_CLASS_REFERENCE_TYPE = "${mp.register.rest.client.class}";

	private static final String MP_REST_ADDED = MicroProfileRegisterRestClientProvider.class.getName() + "#mp-rest";

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(PsiMember psiElement, PsiAnnotation registerRestClientAnnotation,
									 String annotationName, SearchContext context) {
		if (psiElement instanceof PsiClass) {

			IPropertiesCollector collector = context.getCollector();
			if (context.get(MP_REST_ADDED) == null) {

				// FIXME: move this dynamic properties declaration on MicroProfile LS side.
				// /mp-rest/url
				String docs = "The base URL to use for this service, the equivalent of the `baseUrl` method.\r\n"
						+ "This property is considered required, however implementations may have other ways to define these URLs.";
				super.addItemMetadata(collector, MP_REST_CLASS_REFERENCE_TYPE + "/mp-rest/url", "java.lang.String",
						docs, null, null, null, null, null, false);

				// /mp-rest/scope
				docs = "The fully qualified classname to a CDI scope to use for injection, defaults to "
						+ "`javax.enterprise.context.Dependent`.";
				super.addItemMetadata(collector, MP_REST_CLASS_REFERENCE_TYPE + "/mp-rest/scope", "java.lang.String",
						docs, null, null, null, null, null, false);

				// /mp-rest/providers
				docs = "A comma separated list of fully-qualified provider classnames to include in the client, "
						+ "the equivalent of the `register` method or the `@RegisterProvider` annotation.";
				super.addItemMetadata(collector, MP_REST_CLASS_REFERENCE_TYPE + "/mp-rest/providers",
						"java.lang.String", docs, null, null, null, null, null, false);

				context.put(MP_REST_ADDED, Boolean.TRUE);
			}

			PsiClass type = (PsiClass) psiElement;
			ItemHint itemHint = collector.getItemHint(MP_REST_CLASS_REFERENCE_TYPE);
			if (!PsiTypeUtils.isBinary(type)) {
				itemHint.setSource(Boolean.TRUE);
			}

			// Add class annotated with @RegisterRestClient in the "hints" values with name
			// '${mp.register.rest.client.class}'
			ValueHint value = new ValueHint();
			String classOrConfigKey = AnnotationUtils.getAnnotationMemberValue(registerRestClientAnnotation,
					REGISTER_REST_CLIENT_ANNOTATION_CONFIG_KEY);
			if (classOrConfigKey == null) {
				classOrConfigKey = type.getQualifiedName();
			}
			value.setValue(classOrConfigKey);
			value.setSourceType(type.getQualifiedName());
			itemHint.getValues().add(value);
		}
	}
}
