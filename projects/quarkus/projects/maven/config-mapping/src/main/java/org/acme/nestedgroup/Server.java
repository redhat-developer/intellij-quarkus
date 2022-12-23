package org.acme.nestedgroup;

import io.smallrye.config.ConfigMapping;

/**
 * 
 * @see https://quarkus.io/guides/config-mappings#nested-groups
 *
 */
@ConfigMapping(prefix = "server.nestedgroup")
public interface Server {
	String host();

	int port();

	Log log();

	interface Log {
		boolean enabled();

		String suffix();

		boolean rotate();
	}
}