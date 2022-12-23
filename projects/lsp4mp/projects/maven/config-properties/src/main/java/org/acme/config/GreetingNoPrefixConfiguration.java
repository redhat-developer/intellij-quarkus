package org.acme.config;

import java.util.List;

import io.quarkus.arc.config.ConfigProperties;

@ConfigProperties
public class GreetingNoPrefixConfiguration {
	public String message;
	public HiddenConfig hidden;

	public static class HiddenConfig {
		public List<String> recipients;
	}
}