package org.acme.config;

import javax.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@Path("/greeting")
public class DefaultValueResource {

    @ConfigProperty(name = "greeting1", defaultValue="foo")
    int greeting1;

    @ConfigProperty(name = "greeting2", defaultValue="bar")
    Integer greeting2;

    @ConfigProperty(name = "greeting3", defaultValue="1")
    boolean greeting3;

    @ConfigProperty(name = "greeting4", defaultValue="128")
    byte greeting4;

    @ConfigProperty(name = "greeting5", defaultValue="baz")
    String greeting5;

    @ConfigProperty(name = "greeting6", defaultValue="1.0")
    float greeting6;

    @ConfigProperty(name = "greeting7", defaultValue="java.lang.String")
    Class<?> greeting7;

    @ConfigProperty(name = "greeting8", defaultValue="A")
    char greeting8;

    @ConfigProperty(name = "greeting9")
    String greeting9;

    @ConfigProperty(name = "greeting10", defaultValue="AB")
    char greeting10;

    @ConfigProperty(name = "greeting11", defaultValue="1")
    int greeting11;

    @ConfigProperty(name = "greeting12", defaultValue="1")
    Integer greeting12;

    @ConfigProperty(name = "greeting13", defaultValue = "PT15M")
    Duration greeting13;

    @ConfigProperty(name = "greeting14", defaultValue = "PT15")
    Duration greeting14;

    public static enum Profile {
        admin,
        user
    }

    @ConfigProperty(name = "greeting15", defaultValue = "user")
    Profile greeting15;

    @ConfigProperty(name = "greeting16", defaultValue = "userXXX")
    Profile greeting16;
}