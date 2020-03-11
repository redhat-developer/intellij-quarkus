/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.javadoc;

import java.io.Reader;

import org.jsoup.Jsoup;

/**
 * Converts JavaDoc tags into plain text equivalent.
 *
 * @author Fred Bricon
 */
public class JavaDoc2PlainTextConverter extends AbstractJavaDocConverter {

    public JavaDoc2PlainTextConverter(Reader reader) {
        super(reader);
    }

    public JavaDoc2PlainTextConverter(String javadoc) {
        super(javadoc);
    }

    @Override
    String convert(String rawHtml) {
        HtmlToPlainText formatter = new HtmlToPlainText();
        return formatter.getPlainText(Jsoup.parse(rawHtml));
    }
}

