package org.acme.optionals;

import io.smallrye.config.ConfigMapping;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 
 * @see https://quarkus.io/guides/config-mappings#conversions
 *
 */
@ConfigMapping(prefix = "optionals")
public interface Optionals {
	Optional<Server> server();

	Optional<String> optional();

//  dont test naming but only OptionalInt
//	@WithName("optional.int")
	OptionalInt optionalInt();

	interface Server {
		String host();

		int port();
	}
}