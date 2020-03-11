package org.acme.config;

import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class CustomExtensionConfig {

	private String property1;

	private Integer property2;
}
