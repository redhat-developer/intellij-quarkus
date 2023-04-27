package com.demo.rest;

import jakarta.ws.rs.Path;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
@Path("/")
public class IncorrectScopeJakarta {

	@Gauge(name = "Return Int", unit = MetricUnits.NONE, description = "Test method for Gauge annotation")
	public int returnInt() {
		return 2;
	}
}