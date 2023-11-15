package com.redhat.devtools.intellij.quarkus.explorer;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AnimatedIcon;
import com.redhat.devtools.intellij.quarkus.QuarkusBundle;
import com.redhat.devtools.intellij.quarkus.explorer.actions.QuarkusDevStartAction;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;

import javax.swing.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

public class QuarkusRunDevNode extends QuarkusActionNode {

    private static final Icon RUNNING_ICON = new AnimatedIcon.Default();
    private RunConfiguration configuration;

    private long startTime = -1;

    public void setConfiguration(RunConfiguration configuration) {
        this.configuration = configuration;
    }

    public RunConfiguration getConfiguration() {
        return configuration;
    }

    public static enum QuarkusApplicationStatus {
        none,
        starting,
        started,
        stopping,
        stopped;
    }

    private QuarkusApplicationStatus applicationStatus;

    private CompletableFuture<Void> startApplicationStatus;

    public QuarkusRunDevNode(QuarkusProjectNode projectNode) {
        super(QuarkusBundle.message("quarkus.explorer.action.run.dev.mode"), projectNode);
        this.applicationStatus = QuarkusApplicationStatus.none;
    }

    public void setApplicationStatus(QuarkusApplicationStatus applicationStatus) {
        if (startApplicationStatus != null && !startApplicationStatus.isDone()) {
            startApplicationStatus.cancel(true);
        }
        this.applicationStatus = applicationStatus;
        switch (applicationStatus) {
            case starting:
                var projectNode = getProjectNode();
                int port = projectNode.getRunContext().getPort();
                startApplicationStatus = CompletableFutures.computeAsync(cancelChecker -> {
                    while (!cancelChecker.isCanceled()) {
                        if (isServerAvailable("localhost", port, 1000)) {
                            return QuarkusApplicationStatus.started;
                        }
                    }
                    return null;
                }).thenAccept(status -> {
                    if (status == null) {
                        return;
                    }
                    this.applicationStatus = QuarkusApplicationStatus.started;
                    this.add(new QuarkusOpenDevUINode(projectNode));
                    this.add(new QuarkusOpenApplicationNode(projectNode));
                    refreshNode(true);
                });
                startTime = System.currentTimeMillis();
                break;
            case started:
                startTime = -1;
                break;
            case stopping:
                startTime = System.currentTimeMillis();
                break;
            case stopped:
                startTime = -1;
                this.removeAllChildren();
                refreshNode(true);
                break;
            default:
                refreshNode(false);
        }

    }

    @Override
    public Icon getIcon() {
        switch(applicationStatus) {
            case none:
                return super.getIcon();
            case started:
                return AllIcons.Actions.Commit;
            case stopped:
                return AllIcons.Actions.Suspend;
            default:
                return RUNNING_ICON;
        }
    }

    public String getElapsedTime() {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        return StringUtil.formatDuration(duration, "\u2009");
    }

    public QuarkusApplicationStatus getApplicationStatus() {
        return applicationStatus;
    }

    private static boolean isServerAvailable(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String getActionId() {
        return QuarkusDevStartAction.ACTION_ID;
    }
}
