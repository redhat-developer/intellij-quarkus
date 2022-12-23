package org.acme.config;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/greeting/method") 
public class GreetingMethodResource {
  
    String message;
   
    String suffix;

    Optional<String> name;

    @Inject
    public void setMessage(@ConfigProperty(name = "greeting.method.message") String message) {
        this.message = message;
    }

    @Inject
    public void setSuffix(@ConfigProperty(name = "greeting.method.suffix" , defaultValue="!") String suffix) {
        this.suffix = suffix;
    }

    @Inject
    public void setName(@ConfigProperty(name = "greeting.method.name") Optional<String> name) {
        this.name = name;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return message + " " + name.orElse("world") + suffix;
    }
}