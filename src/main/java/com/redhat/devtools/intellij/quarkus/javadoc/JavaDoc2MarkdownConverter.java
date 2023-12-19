/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.javadoc;


import java.io.Reader;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.parser.PegdownExtensions;
import com.vladsch.flexmark.util.data.DataKey;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts JavaDoc tags into Markdown equivalent.
 *
 * @author Fred Bricon
 */
public class JavaDoc2MarkdownConverter extends AbstractJavaDocConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaDoc2MarkdownConverter.class);

    private static final String LINE_SEPARATOR = "\n";

    final static public DataKey<Integer> HTML_EXTENSIONS = new DataKey<>("HTML_EXTENSIONS", 0
            //| Extensions.ABBREVIATIONS
            //| Extensions.EXTANCHORLINKS /*| Extensions.EXTANCHORLINKS_WRAP*/
            //| Extensions.AUTOLINKS
            //| Extensions.DEFINITIONS
            | PegdownExtensions.FENCED_CODE_BLOCKS
            //| Extensions.FORCELISTITEMPARA
            //| Extensions.HARDWRAPS
            //| Extensions.ATXHEADERSPACE
            //| Extensions.QUOTES
            //| Extensions.SMARTS
            //| Extensions.RELAXEDHRULES
            //| Extensions.STRIKETHROUGH
            //| Extensions.SUPPRESS_HTML_BLOCKS
            //| Extensions.SUPPRESS_INLINE_HTML
            //| Extensions.TABLES
            //| Extensions.TASKLISTITEMS
            //| Extensions.WIKILINKS
            //| Extensions.TRACE_PARSER
    );
    private static final FlexmarkHtmlConverter CONVERTER = FlexmarkHtmlConverter.builder().build();

    public JavaDoc2MarkdownConverter(Reader reader) {
        super(reader);
    }

    public JavaDoc2MarkdownConverter(String javadoc) {
        super(javadoc);
    }

    @Override
    public String convert(String html) {
        Document document = Jsoup.parse(html);
        //Add missing table headers if necessary, else most Markdown renderers will crap out
        document.select("table").forEach(JavaDoc2MarkdownConverter::addMissingTableHeaders);

        String markdown = CONVERTER.convert(document);
        if (markdown.endsWith(LINE_SEPARATOR)) {// FlexmarkHtmlConverter keeps adding an extra line
            markdown = markdown.substring(0, markdown.length() - LINE_SEPARATOR.length());
        }

        return markdown;
    }

    /**
     * Adds a new row header if the given table doesn't have any.
     *
     * @param table
     *            the HTML table to check for a header
     */
    private static void addMissingTableHeaders(Element table) {
        int numCols = 0;
        for (Element child : table.children()) {
            if ("thead".equals(child.nodeName())) {
                // Table already has a header, nothing else to do
                return;
            }
            if ("tbody".equals(child.nodeName())) {
                Elements rows = child.getElementsByTag("tr");
                if (!rows.isEmpty()) {
                    for (Element row : rows) {
                        int colSize = row.getElementsByTag("td").size();
                        //Keep the biggest column size
                        if (colSize > numCols) {
                            numCols = colSize;
                        }
                    }
                }
            }
        }
        if (numCols > 0) {
            //Create a new header row based on the number of columns already found
            Element newHeader = new Element("tr");
            for (int i = 0; i < numCols; i++) {
                newHeader.appendChild(new Element("th"));
            }
            //Insert header row in 1st position in the table
            table.insertChildren(0, newHeader);
        }
    }
}
