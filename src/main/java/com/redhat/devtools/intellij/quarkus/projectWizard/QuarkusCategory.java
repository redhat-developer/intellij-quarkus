/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.projectWizard;

import java.util.ArrayList;
import java.util.List;

public class QuarkusCategory {
    private String name;
    private List<QuarkusExtension> extensions = new ArrayList<>();

    public QuarkusCategory(String name) {
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<QuarkusExtension> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<QuarkusExtension> extensions) {
        this.extensions = extensions;
    }
}
