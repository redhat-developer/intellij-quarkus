package org.acme.map;

import java.util.Map;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "server.map")
public interface Server {

	String host();

	int port();

	Map<String, String> form();
}