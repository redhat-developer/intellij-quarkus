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
import java.lang.reflect.Field;

import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

import com.overzealous.remark.Options;
import com.overzealous.remark.Options.Tables;
import com.overzealous.remark.Remark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts JavaDoc tags into Markdown equivalent.
 *
 * @author Fred Bricon
 */
public class JavaDoc2MarkdownConverter extends AbstractJavaDocConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaDoc2MarkdownConverter.class);

    private static Remark remark;

    static {
        Options options = new Options();
        options.tables = Tables.MULTI_MARKDOWN;
        options.hardwraps = true;
        options.inlineLinks = true;
        options.autoLinks = true;
        options.reverseHtmlSmartPunctuation = true;
        remark = new Remark(options);
        //Stop remark from stripping file and jdt protocols in an href
        try {
            Field cleanerField = Remark.class.getDeclaredField("cleaner");
            cleanerField.setAccessible(true);

            Cleaner c = (Cleaner) cleanerField.get(remark);

            Field whitelistField = Cleaner.class.getDeclaredField("whitelist");
            whitelistField.setAccessible(true);

            Whitelist w = (Whitelist) whitelistField.get(c);

            w.addProtocols("a", "href", "file", "jdt");
            w.addProtocols("img", "src", "file");
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            LOGGER.error("Unable to modify jsoup to include file and jdt protocols", e);
        }
    }

    public JavaDoc2MarkdownConverter(Reader reader) {
        super(reader);
    }

    public JavaDoc2MarkdownConverter(String javadoc) {
        super(javadoc);
    }

    @Override
    String convert(String rawHtml) {
        return remark.convert(rawHtml);
    }
}
