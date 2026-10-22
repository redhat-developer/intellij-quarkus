package org.acme.flow;

import io.serverlessworkflow.impl.WorkflowDefinition;
import io.smallrye.common.annotation.Identifier;
import jakarta.inject.Inject;

/**
 * Uses the "class" naming strategy ({@code quarkus.flow.definitions.naming-strategy=class} in
 * {@code application.properties}): {@code namespace.vX_Y_Z.PascalCaseName} - note there is no
 * versionless convenience form here, unlike the default "spec" strategy used by {@link FlowBean}.
 */
public class FlowClassStrategyBean {

    @Inject
    @Identifier("flow.v1_0_0.MultiVersion")
    WorkflowDefinition multiVersion;

}
