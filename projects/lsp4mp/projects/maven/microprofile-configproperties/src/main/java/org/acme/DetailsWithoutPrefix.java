package org.acme;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ConfigProperties
@Dependent
public class DetailsWithoutPrefix {
    public String host2; // the value of the configuration property server.host
    public int port2;   // the value of the configuration property server.port
    private String endpoint2; //the value of the configuration property server.endpoint
    public @ConfigProperty(name="old.location2")
    String location2; //the value of the configuration property server.old.location
    public String getEndpoint() {
        return endpoint2;
    }
}
