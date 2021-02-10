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
package com.redhat.devtools.intellij.quarkus.module;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TableUtil;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuarkusExtensionsStep extends ModuleWizardStep implements Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusExtensionsStep.class);
    private static final Icon CODESTARTS_ICON = IconLoader.findIcon("/images/fighter-jet-solid.svg");

    private JBSplitter panel;
    private final WizardContext wizardContext;

    private class ExtensionsTable extends JBTable {
        private class Model extends AbstractTableModel {
            private List<QuarkusExtension> extensions;

            private Model(List<QuarkusExtension> extensions) {
                this.extensions = extensions;
            }
            @Override
            public int getRowCount() {
                return extensions.size();
            }

            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                QuarkusExtension extension = extensions.get(rowIndex);
                if (columnIndex == 0) {
                    return extension.isDefaultExtension() || extension.isSelected();
                } else {
                    return extension;
                }
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                QuarkusExtension extension = extensions.get(rowIndex);
                return columnIndex == 0 && !extension.isDefaultExtension();
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                QuarkusExtension extension = extensions.get(rowIndex);
                extension.setSelected((Boolean) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0?Boolean.class:String.class;
            }

            public void setExtensions(List<QuarkusExtension> extensions) {
                this.extensions = extensions;
                fireTableDataChanged();
            }

            public QuarkusExtension getElementAt(int index) {
                return extensions.get(index);
            }
        }

        private ExtensionsTable() {
            setShowGrid(false);
            setShowVerticalLines(false);
            this.setCellSelectionEnabled(false);
            this.setRowSelectionAllowed(true);
            this.setSelectionMode(0);
            this.setTableHeader(null);
            setModel(new Model(new ArrayList<>()));
            TableColumn selectedColumn = columnModel.getColumn(0);
            TableUtil.setupCheckboxColumn(this, 0);
            selectedColumn.setCellRenderer(new BooleanTableCellRenderer());
            TableColumn extensionColumn = columnModel.getColumn(1);
            extensionColumn.setCellRenderer(new ColoredTableCellRenderer() {

                @Override
                protected void customizeCellRenderer(JTable table, @Nullable Object value, boolean b, boolean b1, int i, int i1) {
                    QuarkusExtension extension = (QuarkusExtension) value;
                    append(extension.getName(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, null));
                    append(" ");
                    append(extension.asLabel(false), new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, null));
                    if (extension.isProvidesExampleCode()) {
                        setIcon(CODESTARTS_ICON);
                        setIconOnTheRight(true);
                    }
                }
            });
        }

        public void setExtensions(List<QuarkusExtension> extensions) {
            ((Model)getModel()).setExtensions(extensions);
        }
    }

    private class SelectedExtensionsModel extends AbstractListModel<QuarkusExtension> {

        private final List<QuarkusExtension> extensions = new ArrayList<>();

        private SelectedExtensionsModel(List<QuarkusCategory> categories) {
            Set<String> ids = new HashSet<>();
            categories.stream().flatMap(category -> category.getExtensions().stream()).filter(extension -> extension.isDefaultExtension() || extension.isSelected()).forEach(extension -> {
                if (!ids.contains(extension.getId())) {
                    ids.add(extension.getId());
                    extensions.add(extension);
                }
            });
        }

        @Override
        public int getSize() {
            return extensions.size();
        }

        @Override
        public QuarkusExtension getElementAt(int index) {
            return extensions.get(index);
        }
    }

    public QuarkusExtensionsStep(WizardContext wizardContext) {
        this.wizardContext = wizardContext;
    }

    @Override
    public JComponent getComponent() {
        if (panel == null && wizardContext.getUserData(QuarkusConstants.WIZARD_MODEL_KEY) != null) {
            panel = new JBSplitter(false, 0.8f);

            //categories component
            List<QuarkusCategory> categories = wizardContext.getUserData(QuarkusConstants.WIZARD_MODEL_KEY).getCategories();
            JBList<QuarkusCategory> categoriesList = new JBList<>(categories);
            ColoredListCellRenderer<QuarkusCategory> categoryRender = new ColoredListCellRenderer<QuarkusCategory>() {
                @Override
                protected void customizeCellRenderer(@NotNull JList<? extends QuarkusCategory> list, QuarkusCategory category, int index, boolean selected, boolean hasFocus) {
                    append(category.getName());
                }
            };
            categoriesList.setCellRenderer(categoryRender);
            JBSplitter leftPanel = new JBSplitter(false, 0.5f);
            leftPanel.setFirstComponent(new JBScrollPane(categoriesList));

            //extensions component
            ExtensionsTable extensionsTable = new ExtensionsTable();
            JTextPane extensionDetailTextPane = new JTextPane();
            extensionDetailTextPane.setEditorKit(getHtmlEditorKit());
            extensionDetailTextPane.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        BrowserUtil.browse(e.getURL());
                    }
                }
            });
            extensionDetailTextPane.setEditable(false);
            JBSplitter extensionsSplitter = new JBSplitter(true, 0.8f);
            extensionsSplitter.setFirstComponent(new JBScrollPane(extensionsTable));
            extensionsSplitter.setSecondComponent(extensionDetailTextPane);
            leftPanel.setSecondComponent(extensionsSplitter);
            categoriesList.addListSelectionListener(e -> extensionsTable.setExtensions(categoriesList.getSelectedValue().getExtensions()));
            if (!categories.isEmpty()) {
                categoriesList.setSelectedIndex(0);
            }
            panel.setFirstComponent(leftPanel);
            JBList<QuarkusExtension> selectedExtensions = new JBList<>();
            selectedExtensions.setModel(new SelectedExtensionsModel(categories));
            ColoredListCellRenderer<QuarkusExtension> selectedExtensionRenderer = new ColoredListCellRenderer<QuarkusExtension>() {
                @Override
                protected void customizeCellRenderer(@NotNull JList<? extends QuarkusExtension> list, QuarkusExtension extension, int index, boolean selected, boolean hasFocus) {
                    append(extension.getName());
                }
            };
            selectedExtensions.setCellRenderer(selectedExtensionRenderer);
            JPanel selectedExtensionsPanel = new JPanel();
            selectedExtensionsPanel.setLayout(new BoxLayout(selectedExtensionsPanel, BoxLayout.Y_AXIS));
            JLabel label = new JLabel("Selected extensions");
            label.setFont(label.getFont().deriveFont(label.getFont().getStyle() | Font.BOLD));
            selectedExtensionsPanel.add(label);
            selectedExtensionsPanel.add(selectedExtensions);
            panel.setSecondComponent(new JBScrollPane(selectedExtensionsPanel));
            extensionsTable.getModel().addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    if (e.getType() == TableModelEvent.UPDATE) {
                        selectedExtensions.setModel(new SelectedExtensionsModel(categories));
                    }
                }
            });
            extensionsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    int index = extensionsTable.getSelectedRow();
                    if (index >=0) {
                        QuarkusExtension extension = ((ExtensionsTable.Model)extensionsTable.getModel()).getElementAt(index);
                        StringBuilder builder = new StringBuilder("<html><body>" + extension.getDescription() + ".");
                        if (StringUtils.isNotBlank(extension.getGuide())) {
                            builder.append(" <a href=\"" + extension.getGuide() + "\">Click to open guide</a>");
                        }
                        builder.append("</body></html>");
                        extensionDetailTextPane.setText(builder.toString());
                    } else {
                        extensionDetailTextPane.setText("");
                    }
                }
            });
        }
        return panel;
    }

    /**
     * Use reflection to get IntelliJ specific HTML editor kit as it has moved in 2020.1
     *
     * @return the HTML editor kit to use
     */
    @NotNull
    private EditorKit getHtmlEditorKit() {
        try {
            return (EditorKit) Class.forName("com.intellij.util.ui.JBHtmlEditorKit").newInstance();
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            try {
                return (EditorKit) Class.forName("com.intellij.util.ui.UIUtil$JBHtmlEditorKit").newInstance();
            } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e1) {
                LOGGER.warn("Can't create IntelliJ specific editor kit", e1);
                return new HTMLEditorKit();
            }
        }
    }

    @Override
    public void updateDataModel() {

    }

    @Override
    public void dispose() {

    }
}
