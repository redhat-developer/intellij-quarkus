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
package com.redhat.devtools.intellij.quarkus.module;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.TableUtil;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.util.List;

public class QuarkusExtensionsStep extends ModuleWizardStep implements Disposable {
    private JBSplitter panel;
    private final WizardContext wizardContext;

    private class ExtensionsTable extends JBTable {
        private class Model extends AbstractTableModel {
            private final List<QuarkusExtension> extensions;

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
                    return extension.asLabel();
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
        }

        private ExtensionsTable() {
            setShowGrid(false);
            setShowVerticalLines(false);
            this.setCellSelectionEnabled(false);
            this.setRowSelectionAllowed(true);
            this.setSelectionMode(0);
            this.setTableHeader(null);
        }

        public void setExtensions(List<QuarkusExtension> extensions) {
            setModel(new Model(extensions));
            TableColumn selectedColumn = columnModel.getColumn(0);
            TableUtil.setupCheckboxColumn(this, 0);
            selectedColumn.setCellRenderer(new BooleanTableCellRenderer());
            TableColumn extensionColumn = columnModel.getColumn(1);
            extensionColumn.setCellRenderer(new ColoredTableCellRenderer() {

                @Override
                protected void customizeCellRenderer(JTable table, @Nullable Object value, boolean b, boolean b1, int i, int i1) {
                    append(value.toString());
                }
            });
        }
    }

    public QuarkusExtensionsStep(WizardContext wizardContext) {
        this.wizardContext = wizardContext;
    }

    @Override
    public JComponent getComponent() {
        if (panel == null && wizardContext.getUserData(QuarkusConstants.WIZARD_MODEL_KEY) != null) {
            panel = new JBSplitter(false, 0.8f);
            /*Tree modelTree = new Tree();
            modelTree.setModel(new ModelTreeModel(wizardContext.getUserData(QuarkusConstants.WIZARD_MODEL_KEY)));
            modelTree.setCellRenderer(new ModelCellRenderer());
            modelTree.setCellEditor(new ModelCellEditor(modelTree, (DefaultTreeCellRenderer) modelTree.getCellRenderer()));
            panel.setFirstComponent(new JBScrollPane(modelTree));*/
            JBList<QuarkusCategory> categoriesList = new JBList<>(wizardContext.getUserData(QuarkusConstants.WIZARD_MODEL_KEY).getCategories());
            ColoredListCellRenderer<QuarkusCategory> categoryRender = new ColoredListCellRenderer<QuarkusCategory>() {
                @Override
                protected void customizeCellRenderer(@NotNull JList<? extends QuarkusCategory> list, QuarkusCategory category, int index, boolean selected, boolean hasFocus) {
                    append(category.getName());
                }
            };
            categoriesList.setCellRenderer(categoryRender);
            panel.setFirstComponent(new JBScrollPane(categoriesList));
            ExtensionsTable extensionsTable = new ExtensionsTable();
            panel.setSecondComponent(new JBScrollPane(extensionsTable));
            categoriesList.addListSelectionListener(e -> extensionsTable.setExtensions(categoriesList.getSelectedValue().getExtensions()));
        }
        return panel;
    }

    @Override
    public void updateDataModel() {

    }

    @Override
    public void dispose() {

    }
}
