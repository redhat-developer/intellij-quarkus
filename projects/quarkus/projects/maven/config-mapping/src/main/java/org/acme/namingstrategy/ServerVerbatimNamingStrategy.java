package org.acme.namingstrategy;

import static io.smallrye.config.ConfigMapping.NamingStrategy.VERBATIM;

import io.smallrye.config.ConfigMapping;

/**
 * 
 * @see https://quarkus.io/guides/config-mappings#namingstrategy
 *
 */
@ConfigMapping(prefix = "server.verbatim", namingStrategy = VERBATIM)
public interface ServerVerbatimNamingStrategy {

	String theHost();

	int thePort();
}