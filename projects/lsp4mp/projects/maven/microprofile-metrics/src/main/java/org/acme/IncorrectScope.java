package org.acme;

import javax.ws.rs.Path;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;

import javax.enterprise.context.RequestScoped;

@RequestScoped
@Path("/")
public class IncorrectScope {

	@Gauge(name = "Return Int", unit = MetricUnits.NONE, description = "Test method for Gauge annotation")
	public int returnInt() {
		return 2;
	}

}
