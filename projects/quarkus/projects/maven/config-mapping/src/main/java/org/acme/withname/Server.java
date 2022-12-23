package org.acme.withname;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

/**
 * @see https://quarkus.io/guides/config-mappings#withname
 *
 */
@ConfigMapping(prefix = "server.withname")
interface Server {

	@WithName("name")
	String host();

	int port();
}