package com.redhat.devtools.intellij.quarkus.explorer.actions;

import com.intellij.execution.ExecutorRegistryImpl;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import com.redhat.devtools.intellij.quarkus.explorer.QuarkusRunDevNode;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfigurationType;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreePath;
import java.util.List;

public class QuarkusDevStartAction extends QuarkusTreeAction {

    public static final String ACTION_ID = "com.redhat.devtools.intellij.quarkus.explorer.actions.QuarkusDevStartAction";

    public QuarkusDevStartAction() {

    }

    @Override
    protected void actionPerformed(@NotNull Tree tree, @NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();
        QuarkusRunDevNode application = path != null ? (QuarkusRunDevNode) path.getLastPathComponent() : null;
        if (application == null || !(application.getApplicationStatus() == QuarkusRunDevNode.QuarkusApplicationStatus.none || application.getApplicationStatus() == QuarkusRunDevNode.QuarkusApplicationStatus.stopped)) {
            return;
        }
        Module module = getModule(tree);
        if (module == null) {
            return;
        }
        Project project = e.getProject();

        RunnerAndConfigurationSettings quarkusSettings = null;
        List<RunnerAndConfigurationSettings> list = RunManager.getInstance(project).getConfigurationSettingsList(QuarkusRunConfigurationType.class);
        if (!list.isEmpty()) {
            for (RunnerAndConfigurationSettings settings : list) {
                QuarkusRunConfiguration configuration = (QuarkusRunConfiguration) settings.getConfiguration();
                if (module.equals(configuration.getModule())) {
                    quarkusSettings = settings;
                    break;
                }
            }
        }
        if (quarkusSettings == null) {
            quarkusSettings = RunManager.getInstance(project).createConfiguration(module.getName(), QuarkusRunConfigurationType.class);
            ((QuarkusRunConfiguration) quarkusSettings.getConfiguration()).setModule(module);
        }
        var dataContext = e.getDataContext();
        var executor = new DefaultRunExecutor();

        application.setConfiguration(quarkusSettings.getConfiguration());
        ExecutorRegistryImpl.RunnerHelper.run(project, quarkusSettings.getConfiguration(), quarkusSettings, dataContext, executor);
    }

}
