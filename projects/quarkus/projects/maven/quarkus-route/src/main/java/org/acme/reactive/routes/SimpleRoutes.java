package org.acme.reactive.routes;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.vertx.ext.web.RoutingContext;

@RouteBase(path = "simple", produces = "text/plain")
public class SimpleRoutes {

    @Route(path = "ping") // the final path is /simple/ping
    void ping(RoutingContext rc) {
        rc.response().end("pong");
    }
}