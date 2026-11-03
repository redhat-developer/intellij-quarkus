package org.acme.flow;

import io.quarkiverse.flow.Flow;
import io.smallrye.common.annotation.Identifier;
import jakarta.inject.Inject;

/**
 * Covers the constructor parameter injection point, as opposed to {@link FlowBean}'s field
 * injection points.
 */
public class FlowParameterBean {

    private final Flow exprFor;

    @Inject
    public FlowParameterBean(@Identifier("flow:for-task-name-collision") Flow exprFor) {
        this.exprFor = exprFor;
    }

}
