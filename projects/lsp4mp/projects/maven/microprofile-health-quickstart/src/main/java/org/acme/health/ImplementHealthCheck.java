package org.acme.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

public class ImplementHealthCheck implements HealthCheck {

	@Override
	public HealthCheckResponse call() {
		return null;
	}

}
