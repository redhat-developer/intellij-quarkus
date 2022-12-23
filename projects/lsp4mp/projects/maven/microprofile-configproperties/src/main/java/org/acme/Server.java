package org.acme;

import java.util.Map;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperties;

/**
 * 
 * Demonstrate {@link ConfigProperties}
 * 
 * @see https://github.com/quarkusio/quarkus/blob/main/extensions/arc/deployment/src/test/java/io/quarkus/arc/test/config/ConfigPropertiesTest.java
 * 
 */
public class Server {

	@Inject
	@ConfigProperties
	ServerConfigProperties configProperties;
	@Inject
	@ConfigProperties(prefix = "cloud")
	ServerConfigProperties configPropertiesCloud;
	@Inject
	@ConfigProperties(prefix = "")
	ServerConfigProperties configPropertiesEmpty;

	@ConfigProperties(prefix = "server3")
	public static class ServerConfigProperties {
		public String host3;
		public int port3;
		public Map<Integer, String> reasons3;
	}
}
