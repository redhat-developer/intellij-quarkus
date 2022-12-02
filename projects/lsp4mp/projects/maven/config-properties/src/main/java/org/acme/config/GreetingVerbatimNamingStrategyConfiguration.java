package org.acme.config;

import java.util.List;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.arc.config.ConfigProperties.NamingStrategy;

@ConfigProperties(prefix = "greetingVerbatim", namingStrategy = NamingStrategy.VERBATIM)
public class GreetingVerbatimNamingStrategyConfiguration {

	public String message;
	public HiddenConfig hiddenConfig;

	public static class HiddenConfig {
		public List<String> recipients;
	}
}
