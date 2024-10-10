package com.redhat.microprofile.psi.internal.quarkus.core.properties;

public enum NamingStrategy {

    /**
     * The method name is used as is to map the configuration property.
     */
    VERBATIM,

    /**
     * The method name is derived by replacing case changes with a dash to map the configuration property.
     */
    KEBAB_CASE,

    /**
     * The method name is derived by replacing case changes with an underscore to map the configuration property.
     */
    SNAKE_CASE;
}
