package org.acme.config;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/greeting/constructor")
public class GreetingConstructorResource {
  
    String message;
   
    String suffix;

    Optional<String> name;

    @Inject
    public GreetingConstructorResource(
            @ConfigProperty(name = "greeting.constructor.message") String message,
            @ConfigProperty(name = "greeting.constructor.suffix" , defaultValue="!") String suffix,
            @ConfigProperty(name = "greeting.constructor.name") Optional<String> name) {

        this.message = message;
        this.suffix = suffix;
        this.name = name;
    } 

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return message + " " + name.orElse("world") + suffix;
    }
}