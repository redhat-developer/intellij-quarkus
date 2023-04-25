package com.demo.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/api/resource")
public class MyResource {

	/**
	 * this is a documentation
	 * @return
	 */
	@GET
	public String getMy() {
		return "my";
	}

}
