package org.acme.map;

import java.util.Map;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import java.util.logging.Level;

@ConfigMapping(prefix = "server.map")
public interface Server {

	String host();

	int port();

	Map<String, String> form();

    @WithName("category")
    Map<String, CategoryConfig> categories();

    interface CategoryConfig {
        Level level();
    }