package org.acme;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.faulttolerance.Fallback;

@Path("/fault-tolerant")
public class FaultTolerantResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Fallback(fallbackMethod = aaa)
    public String hello() {
        return "hello";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Fallback(fallbackMethod = )
    public String hi() {
        return "hello";
    }

    public String aaa() {
        return "hi";
    }

}
