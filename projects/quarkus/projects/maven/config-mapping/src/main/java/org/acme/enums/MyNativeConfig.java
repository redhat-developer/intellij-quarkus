package org.acme.enums;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "my.native")
public interface MyNativeConfig {

    enum MonitoringOption {
        HEAPDUMP
    }

    Optional<List<MonitoringOption>> monitoring();
}