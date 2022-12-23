package org.acme.config;

import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class CustomExtensionConfig {

	public String property1;

	public Integer property2;
	
	private Integer privatePropertyIgnored;
	
	public static Integer staticPropertyIgnored;
}
