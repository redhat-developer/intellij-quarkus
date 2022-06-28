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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeListener;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class QuarkusExtensionsStep extends ModuleWizardStep implements Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusExtensionsStep.class);
    private static final Icon CODESTARTS_ICON = IconLoader.findIcon("/images/fighter-jet-solid.svg");

    private JPanel panel;
    private final WizardContext wizardContext;

    private class ExtensionsTreeCellRenderer extends CheckboxTree.CheckboxTreeCellRenderer {
        @Override
        public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObject instanceof QuarkusCategory) {
                getTextRenderer().append(((QuarkusCategory) userObject).getName());
            } else if (userObject instanceof QuarkusExtension)  {
                getTextRenderer().append(((QuarkusExtension) userObject).asLabel());
            }
        }
    }

    private class ExtensionsTree extends CheckboxTree {

        public ExtensionsTree(CheckedTreeNode root) {
            super(new ExtensionsTreeCellRenderer(), root);
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
        if (panel == null && wizardContext.getUserData(QuarkusConstants.WIZARD_EXTENSIONS_MODEL_KEY) != null) {
            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            JLabel label1 = new JLabel("Filter extensions");
            label1.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(label1);
            SearchTextField filter = new SearchTextField() {
                @Override
                public Dimension getMaximumSize() {
                    Dimension size = super.getPreferredSize();
                    size.height = JBUI.scale(30);
                    return size;
                }
            };
            filter.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(filter);

            JBSplitter extensionsPanel = new JBSplitter(false, 0.8f);
            extensionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            //categories component
            List<QuarkusCategory> categories = wizardContext.getUserData(QuarkusConstants.WIZARD_EXTENSIONS_MODEL_KEY).getCategories();

            //extensions component
            CheckedTreeNode root = getModel(categories, filter);
            CheckboxTree extensionsTree = new ExtensionsTree(root);
            JTextPane extensionDetailTextPane = new JTextPane();
            extensionDetailTextPane.setEditorKit(getHtmlEditorKit());
            extensionDetailTextPane.setEditable(false);
            JBSplitter extensionsSplitter = new JBSplitter(true, 0.8f);
            extensionsSplitter.setFirstComponent(new JBScrollPane(extensionsTree));
            extensionsSplitter.setSecondComponent(extensionDetailTextPane);
            extensionsPanel.setFirstComponent(extensionsSplitter);
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
            extensionsPanel.setSecondComponent(new JBScrollPane(selectedExtensionsPanel));
            panel.add(extensionsPanel);

            filter.addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(@NotNull DocumentEvent e) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        extensionsTree.setModel(new DefaultTreeModel(getModel(categories, filter)));
                        expandTree(extensionsTree);
                    });
                }
            });
            extensionDetailTextPane.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        BrowserUtil.browse(e.getURL());
                    }
                }
            });
            extensionsTree.addCheckboxTreeListener(new CheckboxTreeListener() {
                @Override
                public void nodeStateChanged(@NotNull CheckedTreeNode node) {
                    ((QuarkusExtension) node.getUserObject()).setSelected(node.isChecked());
                    selectedExtensions.setModel(new SelectedExtensionsModel(categories));
                }
            });
            extensionsTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
                @Override
                public void valueChanged(TreeSelectionEvent e) {
                    if (e.getNewLeadSelectionPath() != null) {
                        Object comp = ((DefaultMutableTreeNode) e.getNewLeadSelectionPath().getLastPathComponent()).getUserObject();
                        if (comp instanceof QuarkusExtension) {
                            StringBuilder builder = new StringBuilder("<html><body>" + ((QuarkusExtension) comp).getDescription() + ".");
                            if (StringUtils.isNotBlank(((QuarkusExtension) comp).getGuide())) {
                                builder.append(" <a href=\"" + ((QuarkusExtension) comp).getGuide() + "\">Click to open guide</a>");
                            }
                            builder.append("</body></html>");
                            extensionDetailTextPane.setText(builder.toString());
                        } else {
                            extensionDetailTextPane.setText("");
                        }
                    }
                }
            });
        }
        return panel;
    }

    private void expandTree(JTree tree) {
            TreeNode root = (TreeNode) tree.getModel().getRoot();
            TreePath rootPath = new TreePath(root);

                Enumeration<? extends TreeNode> enumeration = root.children();
                while (enumeration.hasMoreElements()) {
                    TreeNode treeNode = enumeration.nextElement();
                    TreePath treePath = rootPath.pathByAddingChild(treeNode);
                    tree.expandPath(treePath);
        }
    }

    private CheckedTreeNode getModel(List<QuarkusCategory> categories, SearchTextField filter) {
        Pattern pattern = Pattern.compile(".*" + filter.getText() + ".*", Pattern.CASE_INSENSITIVE);
        CheckedTreeNode root = new CheckedTreeNode();
        for(QuarkusCategory category : categories) {
            DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(category);
            for(QuarkusExtension extension : category.getExtensions()) {
                if (pattern.matcher(extension.getName()).matches()) {
                    CheckedTreeNode extensionNode = new CheckedTreeNode(extension);
                    extensionNode.setChecked(extension.isSelected());
                    categoryNode.add(extensionNode);
                }
            }
            if (categoryNode.getChildCount() > 0) {
                root.add(categoryNode);
            }
        }
        return root;
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
