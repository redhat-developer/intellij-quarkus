package org.acme.qute;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@CheckedTemplate
public class Templates {

	public static native TemplateInstance hello2(String name);
    public static native TemplateInstance hello3(String name);
}
