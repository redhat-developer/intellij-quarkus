package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;
public class EmptyKey {

    @ConfigProperty(name="")
    private String bar;
    
}