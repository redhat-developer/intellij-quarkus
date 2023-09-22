package com.redhat.devtools.intellij.quarkus.explorer;

import com.redhat.devtools.intellij.quarkus.run.QuarkusOpenAppInBrowserAction;
import com.redhat.devtools.intellij.quarkus.run.QuarkusOpenDevUIAction;

public class QuarkusOpenApplicationNode extends QuarkusActionNode {
    public QuarkusOpenApplicationNode(QuarkusProjectNode projectNode) {
        super("Open Application", projectNode);
    }

    @Override
    public String getActionId() {
        return QuarkusOpenAppInBrowserAction.ACTION_ID;
    }
}
