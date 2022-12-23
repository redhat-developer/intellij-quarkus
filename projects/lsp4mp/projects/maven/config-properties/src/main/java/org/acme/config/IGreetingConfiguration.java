package org.acme.config;

import io.quarkus.arc.config.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.Optional;

@ConfigProperties(prefix = "greetingInterface")
public interface IGreetingConfiguration {

    @ConfigProperty(name = "message") 
    String message();

    @ConfigProperty(defaultValue = "!")
    String getSuffix(); 

    Optional<String> getName(); 
}