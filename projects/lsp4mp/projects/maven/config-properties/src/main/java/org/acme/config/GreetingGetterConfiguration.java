package org.acme.config;

import io.quarkus.arc.config.ConfigProperties;
import java.util.Optional;

@ConfigProperties(prefix = "greetingGetter")
public class GreetingGetterConfiguration {

	private String message;
	private String suffix = "!";
	private Optional<String> name;
	private String privateField; // No getter for this private field -> No Quarkus property

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public Optional<String> getName() {
		return name;
	}

	public void setName(Optional<String> name) {
		this.name = name;
	}
}