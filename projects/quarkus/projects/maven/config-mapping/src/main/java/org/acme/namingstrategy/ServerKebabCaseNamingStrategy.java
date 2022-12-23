package org.acme.namingstrategy;

import io.smallrye.config.ConfigMapping;
import static io.smallrye.config.ConfigMapping.NamingStrategy.KEBAB_CASE;
/**
 * 
 * @see https://quarkus.io/guides/config-mappings#namingstrategy
 * 
 */
@ConfigMapping(prefix = "server.kebab", namingStrategy = KEBAB_CASE)
public interface ServerKebabCaseNamingStrategy {
  
	String theHost();

	int thePort();
}