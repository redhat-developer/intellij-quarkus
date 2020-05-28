/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.openapi.module.Module;
import com.intellij.psi.search.SearchScope;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import com.redhat.microprofile.commons.DocumentFormat;

import java.util.HashMap;
import java.util.Map;

/**
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/SearchContext.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/SearchContext.java</a>
 */
public class SearchContext {

    private final Module module;

    private final IPropertiesCollector collector;

    private final IPsiUtils utils;

    private final DocumentFormat documentFormat;

    private final Map<String, Object> cache;

    private final SearchScope scope;

    public SearchContext(Module module, SearchScope scope, IPropertiesCollector collector, IPsiUtils utils,
                         DocumentFormat documentFormat) {
        this.module = module;
        this.scope = scope;
        this.collector = collector;
        this.utils = utils;
        this.documentFormat = documentFormat;
        cache = new HashMap<>();
    }

    public void put(String key, Object value) {
        cache.put(key, value);
    }

    public Object get(String key) {
        return cache.get(key);
    }

    public Module getModule() {
        return module;
    }

    public SearchScope getScope() {
        return scope;
    }

    public IPropertiesCollector getCollector() {
        return collector;
    }

    public IPsiUtils getUtils() {
        return utils;
    }

    public DocumentFormat getDocumentFormat() {
        return documentFormat;
    }
}
