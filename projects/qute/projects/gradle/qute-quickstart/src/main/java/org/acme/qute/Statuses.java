package org.acme.qute;

import io.quarkus.qute.TemplateData;

@TemplateData
@TemplateData(namespace = "FOO")
@TemplateData(namespace = "BAR")
public class Statuses {
    public static final String ON = "on";
    public static final String OFF = "off";

    public static String staticMethod(String state) {
		return state == "on" ? Statuses.ON : Statuses.OFF;
	}
}
