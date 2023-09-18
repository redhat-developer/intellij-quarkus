package org.acme.qute;

import java.util.List;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@CheckedTemplate(basePath="ItemResourceWithFragment")
public class ItemTemplatesCustomBasePath {

	static native TemplateInstance items(List<Item> items);
	static native TemplateInstance items$id1(List<Item> items);
	static native TemplateInstance items3$id2(List<Item> items);
	static native TemplateInstance items3$(List<Item> items);

}