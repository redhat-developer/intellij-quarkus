package org.acme.sample;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.TemplateInstance;

@Path("items")
public class ItemResource {

	@CheckedTemplate(defaultName=CheckedTemplate.ELEMENT_NAME)
	static class Templates {
		static native TemplateInstance HelloWorld(String name);

	}

	@CheckedTemplate(defaultName=CheckedTemplate.HYPHENATED_ELEMENT_NAME)
	static class Templates2 {
		static native TemplateInstance HelloWorld(String name);

	}

	@CheckedTemplate(defaultName=CheckedTemplate.UNDERSCORED_ELEMENT_NAME)
	static class Templates3 {
		static native TemplateInstance HelloWorld(String name);
	}


}
