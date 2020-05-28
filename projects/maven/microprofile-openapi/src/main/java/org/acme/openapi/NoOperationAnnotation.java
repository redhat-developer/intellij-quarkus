package org.acme.openapi;

import java.util.Properties;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@RequestScoped
@Path("/systems")
public class NoOperationAnnotation {

	@GET
	public Response getMyInformation(String hostname) {
		return Response.ok(listContents()).build();
	}

	@GET
	public Response getPropertiesForMyHost() {
		return Response.ok().build();
	}

	private Properties listContents() {
		Properties info = new Properties();
		info.setProperty("Name", "APITest");
		info.setProperty("Desc", "API Test");
		return info;
	}

}