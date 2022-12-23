package org.acme.simple;

import io.smallrye.config.ConfigMapping;

/**
 * @see https://quarkus.io/guides/config-mappings#config-mappings
 */
@ConfigMapping(prefix = "server.simple")
interface Server {

	String host();

	int port();
}