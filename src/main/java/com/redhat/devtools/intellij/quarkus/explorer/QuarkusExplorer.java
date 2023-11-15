package com.redhat.devtools.intellij.quarkus.explorer;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;
import com.redhat.microprofile.psi.quarkus.PsiQuarkusUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import static com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration.QUARKUS_CONFIGURATION;

public class QuarkusExplorer extends SimpleToolWindowPanel implements Disposable {

    private final Tree tree;
    private final Project project;

    public QuarkusExplorer(@NotNull Project project) {
        super(true, true);
        this.project = project;
        tree = buildTree();
        this.setContent(tree);
        load();
    }

    /**
     * Builds the Language server tree
     *
     * @return Tree object of all language servers
     */
    private Tree buildTree() {

        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Quarkus projects");

        Tree tree = new Tree(top);
        tree.setRootVisible(false);
        tree.setCellRenderer(new QuarkusTreeRenderer());

        tree.putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true);

        ((DefaultTreeModel) tree.getModel()).reload(top);


        var doubleClickListener = new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent event) {
                executeAction(tree);
                return false;
            }
        };
        doubleClickListener.installOn(tree);

        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    executeAction(tree);
                }
            }
        });

        MessageBusConnection connection = project.getMessageBus().connect(project);
        connection.subscribe(RunManagerListener.TOPIC, new RunManagerListener() {
            @Override
            public void runConfigurationSelected(@Nullable RunnerAndConfigurationSettings settings) {
                // Do nothing
            }

            @Override
            public void runConfigurationAdded(@NotNull RunnerAndConfigurationSettings settings) {
                // TODO: refresh tree
            }
        });
        connection.subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {

            @Override
            public void processNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
                QuarkusRunDevNode application = findQuarkusApplication(env);
                if (application != null) {
                    application.setApplicationStatus(QuarkusRunDevNode.QuarkusApplicationStatus.stopped);
                }
            }

            @Override
            public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, final @NotNull ProcessHandler handler) {
                QuarkusRunDevNode application = findQuarkusApplication(env);
                if (application != null) {
                    application.setApplicationStatus(QuarkusRunDevNode.QuarkusApplicationStatus.starting);
                }
            }

            @Override
            public void processTerminating(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
                QuarkusRunDevNode application = findQuarkusApplication(env);
                if (application != null) {
                    application.setApplicationStatus(QuarkusRunDevNode.QuarkusApplicationStatus.stopping);
                }
            }

            @Override
            public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
                QuarkusRunDevNode application = findQuarkusApplication(env);
                if (application != null) {
                    application.setApplicationStatus(QuarkusRunDevNode.QuarkusApplicationStatus.stopped);
                }
            }

            private @Nullable QuarkusRunDevNode findQuarkusApplication(@NotNull ExecutionEnvironment env) {
                QuarkusRunConfiguration runConfiguration = env.getDataContext() != null ? (QuarkusRunConfiguration) env.getDataContext().getData(QUARKUS_CONFIGURATION) : null;
                if (runConfiguration == null) {
                    return null;
                }

                Module module = runConfiguration.getModule();
                DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
                Enumeration<TreeNode> children = root.children();
                while (children.hasMoreElements()) {
                    TreeNode node = children.nextElement();
                    if (node instanceof QuarkusProjectNode && module.equals(((QuarkusProjectNode) node).getModule())) {
                        QuarkusProjectNode project = (QuarkusProjectNode) node;
                        Enumeration<TreeNode> children2 = project.children();
                        while (children2.hasMoreElements()) {
                            TreeNode node2 = children2.nextElement();
                            if (node2 instanceof QuarkusRunDevNode && runConfiguration.equals(((QuarkusRunDevNode) node2).getConfiguration())) {
                                return (QuarkusRunDevNode) node2;
                            }
                        }
                        break;
                    }
                }
                return null;
            }
        });
        return tree;
    }

    private void load() {
        var action = ReadAction.nonBlocking(() -> {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) ((DefaultTreeModel) tree.getModel()).getRoot();
            Module[] modules = ModuleManager.getInstance(project).getModules();
            for (Module javaProject : modules) {
                if (PsiQuarkusUtils.isQuarkusProject(javaProject)) {
                    QuarkusProjectNode projectNode = new QuarkusProjectNode(javaProject, tree);
                    root.add(projectNode);
                    // Fill Quarkus actions
                    projectNode.add(new QuarkusRunDevNode(projectNode));
                }
            }
            ((DefaultTreeModel) tree.getModel()).reload(root);
        });
        var executeInSmartMode = DumbService.getInstance(project).isDumb();
        if (executeInSmartMode) {
            action = action.inSmartMode(project);
        }
        action
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private static void executeAction(Tree tree) {
        final TreePath path = tree.getSelectionPath();
        Object node = path.getLastPathComponent();
        if (node instanceof QuarkusActionNode) {
            ActionManager am = ActionManager.getInstance();
            String actionId = ((QuarkusActionNode) node).getActionId();
            if (actionId == null) {
                return;
            }
            AnAction action = am.getAction(actionId);
            if (action != null) {
                action.actionPerformed(new AnActionEvent(null,
                        DataManager.getInstance().getDataContext(tree),
                        ActionPlaces.UNKNOWN, new Presentation(),
                        am, 0));
            }

        }
    }

    @Override
    public void dispose() {

    }
}
