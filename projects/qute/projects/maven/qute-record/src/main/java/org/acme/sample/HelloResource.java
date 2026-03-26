package org.acme.sample;

import io.quarkus.qute.CheckedTemplate;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import io.quarkus.qute.TemplateContents;
import io.quarkus.qute.TemplateInstance;

@Path("hello")
public class HelloResource {

    record Hello(String name) implements TemplateInstance {}

    record Bonjour(String name) implements TemplateInstance {}

    record Status() {}

    @CheckedTemplate(basePath="Foo", defaultName=CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    record HelloWorld(String name) implements TemplateInstance {}

    @TemplateContents("Hello {name}!")
    record HelloWithTemplateContents(String name) implements TemplateInstance {}

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public TemplateInstance get(@QueryParam("name") String name) {
        return new Hello(name).data("foo", 100);
    }
}
