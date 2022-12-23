package org.acme.collections;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "server.collections")
public interface ServerCollections {
    Set<Environment> environments();

    interface Environment {
        String name();

        List<App> apps();

        interface App {
            String name();

            List<String> services();

            Optional<List<String>> databases();
        }
    }
}