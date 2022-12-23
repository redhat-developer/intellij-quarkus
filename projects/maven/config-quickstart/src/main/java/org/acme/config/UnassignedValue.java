package org.acme.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.config.ConfigProperties;

public class UnassignedValue {

	@ConfigProperty(name = "foo")
	private String foo;
	
	@ConfigProperties(prefix = "server")
	private class Server {
		
		@ConfigProperty(name = "url")
		private String url;
	}
}
