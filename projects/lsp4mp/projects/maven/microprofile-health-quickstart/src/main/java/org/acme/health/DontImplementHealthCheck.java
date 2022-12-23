package org.acme.health;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class DontImplementHealthCheck {

	public HealthCheckResponse call() {
		return null;
	}

} 
