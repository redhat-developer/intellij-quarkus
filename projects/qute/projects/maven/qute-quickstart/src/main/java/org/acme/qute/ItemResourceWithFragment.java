package org.acme.qute;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.TemplateInstance;

@Path("items2")
public class ItemResourceWithFragment {

	@CheckedTemplate
	static class Templates {
		static native TemplateInstance items(List<Item> items);
		static native TemplateInstance items$id1(List<Item> items);		
		static native TemplateInstance items3$id2(List<Item> items);
		static native TemplateInstance items3$(List<Item> items);
	}

	@CheckedTemplate(ignoreFragments = true)
	static class Templates2 {
		static native TemplateInstance items2(List<Item> items);
		static native TemplateInstance items2$id1(List<Item> items);		
		static native TemplateInstance items2$id2(List<Item> items);
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public TemplateInstance get() {
		List<Item> items = new ArrayList<>();
		items.add(new Item(new BigDecimal(10), "Apple"));
		items.add(new Item(new BigDecimal(16), "Pear"));
		items.add(new Item(new BigDecimal(30), "Orange"));
		return Templates.items(items);
	}

	/**
	 * This template extension method implements the "discountedPrice" computed
	 * property.
	 */
	@TemplateExtension
	static BigDecimal discountedPrice(Item item) {
		return item.price.multiply(new BigDecimal("0.9"));
	}

}