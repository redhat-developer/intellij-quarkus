package org.acme.config;

import java.time.Duration;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/greeting")
public class GreetingResource {

    @ConfigProperty(name = "greeting.message")
    String message;

    @ConfigProperty(name = "greeting.suffix", defaultValue="!")
    String suffix;

    @ConfigProperty(name = "greeting.name")
    Optional<String> name;

    @ConfigProperty(name = "greeting.defaultValue")
    Duration defaultValue;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return message + " " + name.orElse("world") + suffix;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN) 
    @Path("hello")
    public String hello2() {
        return message + " 2 " + name.orElse("world") + suffix;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("hello4")
    public String hello3() {
        return message + " 4 " + name.orElse("world") + suffix;
    }

    @PATCH
    @Path("hello5")
    public String hello5() {
    }
}
