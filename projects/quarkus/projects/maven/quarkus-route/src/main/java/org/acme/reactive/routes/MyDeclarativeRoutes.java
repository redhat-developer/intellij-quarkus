package org.acme.reactive.routes;

import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.ext.web.RoutingContext;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped 
public class MyDeclarativeRoutes {

    // neither path nor regex is set - match a path derived from the method name
    @Route(methods = Route.HttpMethod.GET) 
    void hello(RoutingContext rc) { 
        rc.response().end("hello");
    }

    @Route(path = "/world")
    String helloWorld() { 
        return "Hello world!";
    }

    @Route(path = "/greetings", methods = Route.HttpMethod.GET)
    void greetingsQueryParam(RoutingExchange ex) { 
        ex.ok("hello " + ex.getParam("name").orElse("world")); 
    }

    @Route(path = "/greetings/:name", methods = Route.HttpMethod.GET) 
    void greetingsPathParam(@Param String name, RoutingExchange ex) {
        ex.ok("hello " + name);
    }
}
