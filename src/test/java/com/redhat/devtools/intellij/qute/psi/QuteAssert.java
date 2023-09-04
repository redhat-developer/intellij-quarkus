/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.psi;

import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelTemplate;
import com.redhat.qute.commons.datamodel.resolvers.MessageResolverData;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;
import org.junit.Assert;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Qute assert.
 *
 * @author Angelo ZERR
 */
public class QuteAssert {

    public static void assertValueResolver(String namespace, String signature, String sourceType,
                                           List<ValueResolverInfo> resolvers) {
        assertValueResolver(namespace, signature, sourceType, null, resolvers);
    }

    public static void assertValueResolver(String namespace, String signature, String sourceType, String named,
                                           List<ValueResolverInfo> resolvers) {
        assertValueResolver(namespace, signature, sourceType, named, false, resolvers);
    }

    public static ValueResolverInfo assertValueResolver(String namespace, String signature, String sourceType, String named,
                                                        boolean globalVariable, List<ValueResolverInfo> resolvers) {
        Optional<ValueResolverInfo> result = resolvers.stream()
                .filter(r -> signature.equals(r.getSignature()) && Objects.equals(namespace, r.getNamespace()))
                .findFirst();
        Assert.assertFalse("Find '" + signature + "' value resolver.", result.isEmpty());
        ValueResolverInfo resolver = result.get();
        Assert.assertEquals(namespace, resolver.getNamespace());
        Assert.assertEquals(signature, resolver.getSignature());
        Assert.assertEquals(sourceType, resolver.getSourceType());
        Assert.assertEquals(globalVariable, resolver.isGlobalVariable());
        return resolver;
    }

    public static void assertNotValueResolver(String namespace, String signature, String sourceType, String named,
                                              List<ValueResolverInfo> resolvers) {
        assertNotValueResolver(namespace, signature, sourceType, named, false, resolvers);
    }

    public static void assertNotValueResolver(String namespace, String signature, String sourceType, String named,
                                              boolean globalVariable, List<ValueResolverInfo> resolvers) {
        Optional<ValueResolverInfo> result = resolvers.stream().filter(r -> signature.equals(r.getSignature()))
                .findFirst();
        Assert.assertTrue("Find '" + signature + "' value resolver.", result.isEmpty());
    }

    public static void assertMessageResolverData(String locale, String messageContent, ValueResolverInfo message) {
        if (locale == null && messageContent == null) {
            Assert.assertNull(message.getData());
        } else {
            Assert.assertNotNull(message.getData());
            Assert.assertTrue(message.getData() instanceof MessageResolverData);
            MessageResolverData data = (MessageResolverData) message.getData();
            Assert.assertEquals(locale, data.getLocale());
            Assert.assertEquals(messageContent, data.getMessage());
        }
    }

    public static void assertParameter(String key, String sourceType, boolean dataMethodInvocation,
                                       List<DataModelParameter> parameters, int index) {
        DataModelParameter parameter = parameters.get(index);
        Assert.assertEquals(key, parameter.getKey());
        Assert.assertEquals(sourceType, parameter.getSourceType());
        Assert.assertEquals(dataMethodInvocation, parameter.isDataMethodInvocation());
    }

    public static void assertParameter(String key, String sourceType, boolean dataMethodInvocation,
                                       DataModelTemplate<DataModelParameter> template) {
        DataModelParameter parameter = template.getParameter(key);
        Assert.assertNotNull(parameter);
        Assert.assertEquals(key, parameter.getKey());
        Assert.assertEquals(sourceType, parameter.getSourceType());
        Assert.assertEquals(dataMethodInvocation, parameter.isDataMethodInvocation());
    }


}