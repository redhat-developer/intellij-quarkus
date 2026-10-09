/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.projectWizard;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuarkusExtensionsModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusExtensionsModel.class);

    private final String key;
    private final List<QuarkusCategory> categories = new ArrayList<>();

    public QuarkusExtensionsModel(String key, List<QuarkusExtension> extensions) {
        this.key = key;
        extensions.sort(Comparator.comparingInt(QuarkusExtension::getOrder));
        Map<String, QuarkusExtension> extensionIds = new HashMap<>();
        final QuarkusCategory[] currentCategory = {null};
        extensions.forEach(e -> {
            String categoryName = e.getCategoryName();
            if (categoryName == null || categoryName.isEmpty()) {
                LOGGER.warn("Extension '{}' has no category, skipping it", e.getId());
                return;
            }
            if (currentCategory[0] == null || !Objects.equals(categoryName, currentCategory[0].getName())) {
                currentCategory[0] = new QuarkusCategory(categoryName);
                categories.add(currentCategory[0]);
            }
            if (extensionIds.containsKey(e.getId())) {
                currentCategory[0].getExtensions().add(extensionIds.get(e.getId()));
            } else {
                currentCategory[0].getExtensions().add(e);
                extensionIds.put(e.getId(), e);
            }
        });
    }

    public String getKey() {
        return key;
    }

    public List<QuarkusCategory> getCategories() {
        return categories;
    }
}
