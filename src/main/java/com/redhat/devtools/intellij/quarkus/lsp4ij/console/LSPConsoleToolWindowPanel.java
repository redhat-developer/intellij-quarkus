/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lsp4ij.console;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.CardLayoutPanel;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.redhat.devtools.intellij.quarkus.lsp4ij.console.explorer.LanguageServerExplorer;
import com.redhat.devtools.intellij.quarkus.lsp4ij.console.explorer.LanguageServerProcessTreeNode;

import javax.swing.*;

/**
 * LSP consoles
 */
public class LSPConsoleToolWindowPanel extends SimpleToolWindowPanel implements Disposable {

    private final Project project;

    private LanguageServerExplorer explorer;

    private ConsolesPanel consoles;
    private boolean disposed;

    public LSPConsoleToolWindowPanel(Project project) {
        super(false, true);
        this.project = project;
        createUI();
    }

    private void createUI() {
        explorer = new LanguageServerExplorer(this);
        var scrollPane = new JBScrollPane(explorer);
        this.consoles = new ConsolesPanel();
        var splitPane = createSplitPanel(scrollPane, consoles);
        super.setContent(splitPane);
        super.revalidate();
        super.repaint();
    }

    public Project getProject() {
        return project;
    }

    private static JComponent createSplitPanel(JComponent left, JComponent right) {
        OnePixelSplitter splitter = new OnePixelSplitter(false, 0.15f);
        splitter.setShowDividerControls(true);
        splitter.setHonorComponentsMinimumSize(true);
        splitter.setFirstComponent(left);
        splitter.setSecondComponent(right);
        return splitter;
    }

    public void selectConsole(LanguageServerProcessTreeNode processTreeNode) {
        if (consoles == null || isDisposed()) {
            return;
        }
        consoles.select(processTreeNode, true);
    }

    /**
     * A card-panel that displays panels for each language server instances.
     */
    private class ConsolesPanel extends CardLayoutPanel<LanguageServerProcessTreeNode, LanguageServerProcessTreeNode, LSPConsoleToolWindowPanel.ConsoleOrErrorPanel> {

        @Override
        protected LanguageServerProcessTreeNode prepare(LanguageServerProcessTreeNode key) {
            return key;
        }

        @Override
        protected LSPConsoleToolWindowPanel.ConsoleOrErrorPanel create(LanguageServerProcessTreeNode key) {
            if (isDisposed() || LSPConsoleToolWindowPanel.this.isDisposed()) {
                return null;
            }
            return new LSPConsoleToolWindowPanel.ConsoleOrErrorPanel();
        }

        @Override
        public void dispose() {
            removeAll();
        }

        @Override
        protected void dispose(LanguageServerProcessTreeNode key, LSPConsoleToolWindowPanel.ConsoleOrErrorPanel value) {
            if (value != null) {
                value.dispose();
            }
        }
    }

    private class ConsoleOrErrorPanel extends SimpleCardLayoutPanel<JComponent> {

        private static final String NAME_VIEW_CONSOLE = "console";

        private final ConsoleView consoleView;

        public ConsoleOrErrorPanel() {
            consoleView = createConsoleView(project);
            add(consoleView.getComponent(), NAME_VIEW_CONSOLE);
            showConsole();
        }

        private void showConsole() {
            show(NAME_VIEW_CONSOLE);
        }

        public void showMessage(String message) {
            consoleView.print(message, ConsoleViewContentType.SYSTEM_OUTPUT);
        }
    }

    private ConsoleView createConsoleView(Project project) {
        var builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        builder.setViewer(true);
        return builder.getConsole();
    }

    public void showMessage(LanguageServerProcessTreeNode processTreeNode, String message) {
        if (isDisposed()) {
            return;
        }
        var consoleOrErrorPanel = consoles.getValue(processTreeNode, true);
        if (consoleOrErrorPanel != null) {
            consoleOrErrorPanel.showMessage(message);
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        explorer.dispose();
    }

    private boolean isDisposed() {
        return disposed || project.isDisposed();
    }
}
