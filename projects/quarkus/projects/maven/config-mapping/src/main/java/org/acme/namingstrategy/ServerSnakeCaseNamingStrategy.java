package org.acme.namingstrategy;

import static io.smallrye.config.ConfigMapping.NamingStrategy.SNAKE_CASE;

import io.smallrye.config.ConfigMapping;

/**
 * 
 * @see https://quarkus.io/guides/config-mappings#namingstrategy
 *
 */
@ConfigMapping(prefix = "server.snake", namingStrategy = SNAKE_CASE)
public interface ServerSnakeCaseNamingStrategy {

	String theHost();

	int thePort();
}