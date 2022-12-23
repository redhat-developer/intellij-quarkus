 package org.acme;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ConfigProperties(prefix="server")
@Dependent
public class Details {
    public String host; // the value of the configuration property server.host
    public int port;   // the value of the configuration property server.port
    private String endpoint; //the value of the configuration property server.endpoint
    public @ConfigProperty(name="old.location")
    String location; //the value of the configuration property server.old.location
    public String getEndpoint() {
        return endpoint;
    }
}
