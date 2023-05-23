package org.acme.qute;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Path("/hello")
public class HelloResource {

	@Inject
	Template hello;

	@Inject
	Template goodbye;

	@Location("detail/items2_v1.html")
	@Inject
	Template hallo;

	@Inject
	Template bonjour;

	@Inject
	Template aurevoir;

	public HelloResource(@Location("detail/page1.html") Template page1, @Location("detail/page2.html") Template page2) {
		this.bonjour = page1;
		this.aurevoir = requireNonNull(page2, "page is required");
	}

	private Template requireNonNull(Template page2, String string) {
		return null;
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public TemplateInstance get(@QueryParam("name") String name) {
		return hello.data("height", 1.50, "weight", 50L)
				.data("age", 12)					
				.data("name", name);
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public TemplateInstance get2(@QueryParam("name") String name) {
		goodbye.data("age2", 12);
		return goodbye.data("name2", name);
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public TemplateInstance get3(@QueryParam("name") String name) {
		hallo.data("age3", 12);
		return hallo.data("name3", name);
	}
}
