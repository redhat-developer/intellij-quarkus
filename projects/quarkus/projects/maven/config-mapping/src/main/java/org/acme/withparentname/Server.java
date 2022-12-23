package org.acme.withparentname;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

/**
 * 
 * @see https://quarkus.io/guides/config-mappings#withparentname
 */
@ConfigMapping(prefix = "server.withparentname")
public interface Server {

	@WithParentName
	ServerHostAndPort hostAndPort();

	@WithParentName
	ServerInfo info();
}