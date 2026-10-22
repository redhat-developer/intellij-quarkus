package org.acme.flow;

import io.quarkiverse.flow.Flow;
import io.smallrye.common.annotation.Identifier;
import jakarta.inject.Inject;

/**
 * Uses the default "spec" naming strategy: {@code namespace:name[:version]}.
 * See {@link FlowClassStrategyBean} for the "class" naming strategy
 * ({@code quarkus.flow.definitions.naming-strategy=class}) instead.
 */
public class FlowBean {

    @Inject
    @Identifier("flow:for-task-name-collision")
    Flow exprFor;

    @Inject
    @Identifier("flow:multi-version")
    Flow multiVersionFlow;

    @Inject
    @Identifier("unrelated-bean")
    String notAFlow;

}
