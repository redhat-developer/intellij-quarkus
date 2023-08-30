package org.acme.reactive.routes;

import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;

public class MultipleRoutes {

    @Route(path = "/first")
    @Route(path = "/second")
    public void route(RoutingContext rc) {
        // ...
    }

}
