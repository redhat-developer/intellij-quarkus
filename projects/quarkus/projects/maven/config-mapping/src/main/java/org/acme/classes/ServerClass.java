package org.acme.classes;

import java.util.Map;

import io.smallrye.config.ConfigMapping;
import java.util.logging.Level;

@ConfigMapping(prefix = "server.classes")
public interface ServerClass {

	Level level();
}