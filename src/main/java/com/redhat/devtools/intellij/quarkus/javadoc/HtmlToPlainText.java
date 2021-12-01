/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 * Jonathan Hedley <jonathan@hedley.net>
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.javadoc;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * HTML to plain-text. Uses jsoup to convert HTML input to lightly-formatted
 * plain-text. This is a fork of Jsoup's <a href=
 * "https://github.com/jhy/jsoup/blob/842977c381b8d48bf12719e3f5cf6fd669379957/src/main/java/org/jsoup/examples/HtmlToPlainText.java">HtmlToPlainText</a>
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class HtmlToPlainText {

    /**
     * Format an Element to plain-text
     * @param element the root element to format
     * @return formatted text
     */
    public String getPlainText(Element element) {
        FormattingVisitor formatter = new FormattingVisitor();
        NodeTraversor.traverse(formatter, element);

        return formatter.toString();
    }

    // the formatting rules, implemented in a breadth-first DOM traverse
    private class FormattingVisitor implements NodeVisitor {
        private StringBuilder accum = new StringBuilder(); // holds the accumulated text
        private int listNesting;
        // hit when the node is first seen
        @Override
        public void head(Node node, int depth) {
            String name = node.nodeName();
            if (node instanceof TextNode) {
                append(((TextNode) node).text()); // TextNodes carry all user-readable text in the DOM.
            } else if (name.equals("ul")) {
                listNesting++;
            } else if (name.equals("li")) {
                append("\n ");
                for (int i = 1; i < listNesting; i++) {
                    append("  ");
                }
                if (listNesting == 1) {
                    append("* ");
                } else {
                    append("- ");
                }
            } else if (name.equals("dt")) {
                append("  ");
            } else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr")) {
                append("\n");
            }
        }

        // hit when all of the node's children (if any) have been visited
        @Override
        public void tail(Node node, int depth) {
            String name = node.nodeName();
            if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5")) {
                append("\n");
            } else if (StringUtil.in(name, "th", "td")) {
                append(" ");
            } else if (name.equals("a")) {
                append(String.format(" <%s>", node.absUrl("href")));
            } else if (name.equals("ul")) {
                listNesting--;
            }
        }

        // appends text to the string builder with a simple word wrap method
        private void append(String text) {
            if (text.equals(" ") &&
                    (accum.length() == 0 || StringUtil.in(accum.substring(accum.length() - 1), " ", "\n")))
            {
                return; // don't accumulate long runs of empty spaces
            }
            accum.append(text);
        }

        @Override
        public String toString() {
            return accum.toString();
        }
    }
}