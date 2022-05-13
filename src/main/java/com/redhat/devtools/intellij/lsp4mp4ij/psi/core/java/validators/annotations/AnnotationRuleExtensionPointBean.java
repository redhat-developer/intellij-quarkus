/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.annotations;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.XCollection;

public class AnnotationRuleExtensionPointBean {
    @Attribute
    public String annotation;

    @Attribute
    public String source;

    @Property(surroundWithTag = false)
    @XCollection
    public AnnotationRuleAttributeExtensionPointBean[] attributes;
}
