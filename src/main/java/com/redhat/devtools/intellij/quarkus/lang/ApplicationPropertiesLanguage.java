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
package com.redhat.devtools.intellij.quarkus.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.properties.PropertiesSyntaxHighlighterFactory;

public class ApplicationPropertiesLanguage extends Language {
    public static final ApplicationPropertiesLanguage INSTANCE = new ApplicationPropertiesLanguage();

    protected ApplicationPropertiesLanguage() {
        super("Quarkus properties", "text/properties");
        SyntaxHighlighterFactory.LANGUAGE_FACTORY.addExplicitExtension(this, new PropertiesSyntaxHighlighterFactory());
    }
}
