package org.acme.config;

import java.util.List;

import io.quarkus.arc.config.ConfigProperties;

@ConfigProperties(prefix = "greetingStackOverflow")
public class GreetingStackOverflowConfiguration {
    public String message;
    public HiddenConfig hidden;

    public static class HiddenConfig {
        public List<String> recipients;
        public HiddenConfig config;
    }
}