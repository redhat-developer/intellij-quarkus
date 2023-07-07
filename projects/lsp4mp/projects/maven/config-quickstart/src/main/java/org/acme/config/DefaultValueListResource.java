package org.acme.config;

import javax.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.List;
import java.util.Set;

@Path("/greetings")
public class DefaultValueListResource {

    @ConfigProperty(name = "listprop1", defaultValue="foo")
    List<Integer> greeting1;

    @ConfigProperty(name = "listprop2", defaultValue="12,13,14")
    List<Integer> greeting2;

    @ConfigProperty(name = "listprop3", defaultValue="12,13,14")
    int[] greeting3;

    @ConfigProperty(name = "listprop4", defaultValue="12,13\\,14")
    int[] greeting4;

    @ConfigProperty(name = "listprop5", defaultValue="1")
    List<Boolean> greeting5;

    @ConfigProperty(name = "listprop6", defaultValue=",,,,,,,,")
    Set<Boolean> greeting6;

    @ConfigProperty(name = "listprop7", defaultValue="1.0,2.0,3.0")
    float[] greeting7;

    @ConfigProperty(name = "listprop8", defaultValue="AB,CD")
    char[] greeting8;

    @ConfigProperty(name = "listprop9", defaultValue=",,,,")
    char[] greeting9;

    @ConfigProperty(name = "listprop10", defaultValue="")
    List<String> greeting10;

}