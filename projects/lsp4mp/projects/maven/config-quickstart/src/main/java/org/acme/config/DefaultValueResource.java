package org.acme.config;

import javax.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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

    @ConfigProperty(name = "greeting.optional") // this optional properties are not set
    java.util.Optional<String> optional;

    @ConfigProperty(name = "greeting.optional.int")
    java.util.OptionalInt optional;

    @ConfigProperty(name = "greeting.optional.long")
    java.util.OptionalLong optional;

    @ConfigProperty(name = "greeting.optional.double")
    java.util.OptionalDouble optional;
}
