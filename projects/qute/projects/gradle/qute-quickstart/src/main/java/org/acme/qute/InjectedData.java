package org.acme.qute;

import javax.inject.Named;

@Named
public class InjectedData {

	@Named
	private String foo;

	@Named("bar")
	private String aBar;

	@Named("user")
	public String getUser() {
		return null;
	}

	@Named
	public String getSystemUser() {
		return null;
	}
}
