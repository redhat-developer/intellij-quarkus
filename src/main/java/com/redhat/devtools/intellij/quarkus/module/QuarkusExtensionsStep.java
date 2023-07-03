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
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
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
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class QuarkusExtensionsStep extends ModuleWizardStep implements Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusExtensionsStep.class);
    private static final Icon PLATFORM_ICON = IconLoader.findIcon("/images/platform-icon.svg", QuarkusExtensionsStep.class);

    private JPanel outerPanel;
    private final WizardContext wizardContext;

    private static class ExtensionsTreeCellRenderer extends CheckboxTree.CheckboxTreeCellRenderer {

        @Override
        public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObject instanceof QuarkusCategory) {
                getTextRenderer().append(((QuarkusCategory) userObject).getName());
            } else if (userObject instanceof QuarkusExtension)  {
                getTextRenderer().append(((QuarkusExtension) userObject).asLabel());
                if (((QuarkusExtension) userObject).isPlatform()) {
                    getTextRenderer().setIcon(PLATFORM_ICON);
                }
            }
        }
    }

    private static class SelectedExtensionsCellRenderer extends ColoredListCellRenderer<QuarkusExtension> {

        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends QuarkusExtension> list, QuarkusExtension extension, int index, boolean selected, boolean hasFocus) {
            append(extension.getName());
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends QuarkusExtension> list, QuarkusExtension value, int index, boolean selected, boolean hasFocus) {
            super.getListCellRendererComponent(list, value, index, selected, hasFocus);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            return this;
        }
    }

    private static class ExtensionsTree extends CheckboxTree {

        public ExtensionsTree(CheckedTreeNode root) {
            super(new ExtensionsTreeCellRenderer(), root);
        }
    }

    private static class SelectedExtensionsModel extends AbstractListModel<QuarkusExtension> {

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
        if (outerPanel == null && wizardContext.getUserData(QuarkusConstants.WIZARD_EXTENSIONS_MODEL_KEY) != null) {
            outerPanel = new JPanel();
            outerPanel.setLayout(new BorderLayout());

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(JBUI.Borders.empty(20));


            JLabel label1 = new JLabel("Filter extensions");
            label1.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(label1);
            SearchTextField filter = new SearchTextField() {
                @Override
                public Dimension getMaximumSize() {
                    Dimension maxSize = super.getMaximumSize();
                    return new Dimension(maxSize.width, JBUI.scale(30));
                }
            };
            filter.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(filter);

            JCheckBox platformCheckbox = new JCheckBox();
            platformCheckbox.setSelected(true);
            platformCheckbox.setText("Platform only extensions");
            panel.add(platformCheckbox);

            JBSplitter extensionsPanel = new JBSplitter(false, 0.8f);
            extensionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            //categories component
            List<QuarkusCategory> categories = wizardContext.getUserData(QuarkusConstants.WIZARD_EXTENSIONS_MODEL_KEY).getCategories();

            //extensions component
            CheckedTreeNode root = getModel(categories, filter, platformCheckbox.isSelected());
            CheckboxTree extensionsTree = new ExtensionsTree(root);
            JTextPane extensionDetailTextPane = new JTextPane();
            extensionDetailTextPane.setEditorKit(getHtmlEditorKit());
            extensionDetailTextPane.setEditable(false);
            JBSplitter extensionsSplitter = new JBSplitter(true, 0.8f);
            extensionsSplitter.setFirstComponent(new JBScrollPane(extensionsTree));
            extensionsSplitter.setSecondComponent(extensionDetailTextPane);
            extensionsPanel.setFirstComponent(extensionsSplitter);

            JBList<QuarkusExtension> selectedExtensions = new JBList<>();
            selectedExtensions.setBackground(null);
            selectedExtensions.setAlignmentX(Component.LEFT_ALIGNMENT);
            selectedExtensions.setModel(new SelectedExtensionsModel(categories));
            selectedExtensions.setCellRenderer(new SelectedExtensionsCellRenderer());

            JPanel selectedExtensionsPanel = new JPanel();
            selectedExtensionsPanel.setLayout(new BoxLayout(selectedExtensionsPanel, BoxLayout.Y_AXIS));
            selectedExtensionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel label = new JLabel("Selected extensions");
            label.setFont(label.getFont().deriveFont(label.getFont().getStyle() | Font.BOLD));
            selectedExtensionsPanel.add(label);

            selectedExtensionsPanel.add(selectedExtensions);
            extensionsPanel.setSecondComponent(new JBScrollPane(selectedExtensionsPanel));
            panel.add(extensionsPanel);

            filter.addDocumentListener(onDocumentChanged(filter, platformCheckbox, categories, extensionsTree));
            platformCheckbox.addItemListener(onItemChanged(filter, platformCheckbox, categories, extensionsTree));
            extensionDetailTextPane.addHyperlinkListener(onHyperlinkClicked());

            extensionsTree.addCheckboxTreeListener(onNodeCheckedStateChanged(categories, selectedExtensions));

            //(Un)Check extension on double-click
            extensionsTree.addMouseListener(onAvailableExtensionClicked(extensionsTree));

            //Unselect extensions on double-click
            selectedExtensions.addMouseListener(onSelectedExtensionClicked(categories, extensionsTree, selectedExtensions));

            //Unselect extensions when pressing the DELETE or BACKSPACE key
            selectedExtensions.addKeyListener(onSelectedExtensionsKeyPressed(categories, extensionsTree, selectedExtensions));

            extensionsTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
                @Override
                public void valueChanged(TreeSelectionEvent e) {
                    if (e.getNewLeadSelectionPath() != null) {
                        Object comp = ((DefaultMutableTreeNode) e.getNewLeadSelectionPath().getLastPathComponent()).getUserObject();
                        if (comp instanceof QuarkusExtension) {
                            QuarkusExtension extension = (QuarkusExtension) comp;
                            StringBuilder builder = new StringBuilder("<html><body>").append(extension.getDescription()).append(".");
                            if (StringUtils.isNotBlank(extension.getGuide())) {
                                builder.append(" <a href=\"").append(extension.getGuide()).append("\">Click to open guide</a>");
                            }
                            builder.append("</body></html>");
                            extensionDetailTextPane.setText(builder.toString());
                        } else {
                            extensionDetailTextPane.setText("");
                        }
                    }
                }
            });
            outerPanel.add(panel, BorderLayout.CENTER);
        }
        return outerPanel;
    }

    private KeyListener onSelectedExtensionsKeyPressed(List<QuarkusCategory> categories, CheckboxTree extensionsTree, JList<QuarkusExtension> selectedExtensions) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    boolean requiresModelRefresh = false;
                    for (QuarkusExtension extension : selectedExtensions.getSelectedValuesList()) {
                        requiresModelRefresh = unselectExtension(extensionsTree, extension) || requiresModelRefresh;
                    }
                    selectedExtensions.clearSelection();
                    if (requiresModelRefresh) {
                        // Some extensions were not visible in the tree so didn't trigger a selectedExtension model refresh
                        // so we force it manually
                        selectedExtensions.setModel(new SelectedExtensionsModel(categories));
                    }
                }
            }
        };
    }

    @NotNull
    private MouseAdapter onSelectedExtensionClicked(List<QuarkusCategory> categories, CheckboxTree extensionsTree, JBList<QuarkusExtension> selectedExtensions) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedIndex = selectedExtensions.getSelectedIndex();
                    if (selectedIndex > -1) {
                        QuarkusExtension extension = selectedExtensions.getModel().getElementAt(selectedIndex);
                        if (unselectExtension(extensionsTree, extension)) {
                            // The extensions was not visible in the tree so didn't trigger a selectedExtension model refresh
                            // so we force it manually
                            selectedExtensions.setModel(new SelectedExtensionsModel(categories));
                        }
                        ;
                    }
                }
            }
        };
    }

    @NotNull
    private static MouseAdapter onAvailableExtensionClicked(CheckboxTree extensionsTree) {
        return new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = extensionsTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null && path.getLastPathComponent() instanceof CheckedTreeNode) {
                        var treeNode = (CheckedTreeNode) path.getLastPathComponent();
                        extensionsTree.setNodeState(treeNode, !treeNode.isChecked());
                    }
                }
            }
        };
    }

    @NotNull
    private static CheckboxTreeListener onNodeCheckedStateChanged(List<QuarkusCategory> categories, JBList<QuarkusExtension> selectedExtensions) {
        return new CheckboxTreeListener() {
            @Override
            public void nodeStateChanged(@NotNull CheckedTreeNode node) {
                QuarkusExtension extension = (QuarkusExtension) node.getUserObject();
                if (extension == null) {
                    // Since ExtensionsTree doesn't extend CheckboxTreeBase directly,
                    // you can't customize its CheckboxTreeBase.CheckPolicy,
                    // so CheckboxTreeHelper.adjustParentsAndChildren basically calls nodeStateChanged(node.getParent())
                    // which doesn't hold a QuarkusExtension and leads to https://github.com/redhat-developer/intellij-quarkus/issues/639
                    // So we bail here.
                    return;
                }
                extension.setSelected(node.isChecked());
                selectedExtensions.setModel(new SelectedExtensionsModel(categories));
            }
        };
    }

    @NotNull
    private static HyperlinkListener onHyperlinkClicked() {
        return new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    BrowserUtil.browse(e.getURL());
                }
            }
        };
    }

    @NotNull
    private ItemListener onItemChanged(SearchTextField filter, JCheckBox platformCheckbox, List<QuarkusCategory> categories, CheckboxTree extensionsTree) {
        return e -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                extensionsTree.setModel(new DefaultTreeModel(getModel(categories, filter, platformCheckbox.isSelected())));
                expandTree(extensionsTree);
            });
        };
    }

    @NotNull
    private DocumentAdapter onDocumentChanged(SearchTextField filter, JCheckBox platformCheckbox, List<QuarkusCategory> categories, CheckboxTree extensionsTree) {
        return new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    extensionsTree.setModel(new DefaultTreeModel(getModel(categories, filter, platformCheckbox.isSelected())));
                    expandTree(extensionsTree);
                });
            }
        };
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

    private CheckedTreeNode getModel(List<QuarkusCategory> categories, SearchTextField filter, boolean platform) {
        Pattern pattern = Pattern.compile(".*" + filter.getText() + ".*", Pattern.CASE_INSENSITIVE);
        CheckedTreeNode root = new CheckedTreeNode();
        for(QuarkusCategory category : categories) {
            DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(category);
            for(QuarkusExtension extension : category.getExtensions()) {
                if (pattern.matcher(extension.getName()).matches() && (!platform || extension.isPlatform())) {
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
     * Unselects a selected extension from the extension tree. Returns true if the extension was not found in the tree, false otherwise.
     */
    private boolean unselectExtension(@NotNull CheckboxTree extensionsTree, @NotNull QuarkusExtension extension) {
        var treeNodes = findTreeNodesForExtension(extensionsTree, extension);
        for (var treeNode : treeNodes) {
            extensionsTree.setNodeState(treeNode, false);
        }
        extension.setSelected(false);
        return treeNodes.isEmpty();
    }

    /**
     * Find CheckedTreeNode for a given extension, as it can belong to several categories
     */
    private @NotNull Set<CheckedTreeNode> findTreeNodesForExtension(@NotNull CheckboxTree extensionsTree, @NotNull QuarkusExtension extension) {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) extensionsTree.getModel().getRoot();
        Enumeration<TreeNode> enumeration = rootNode.depthFirstEnumeration();
        Set<CheckedTreeNode> nodes = new HashSet<>();
        while (enumeration.hasMoreElements()) {
            TreeNode node = enumeration.nextElement();
            if (node instanceof CheckedTreeNode && ((CheckedTreeNode)node).getUserObject() == extension) {
                nodes.add( (CheckedTreeNode)node);
            }
        }
        return nodes;
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
