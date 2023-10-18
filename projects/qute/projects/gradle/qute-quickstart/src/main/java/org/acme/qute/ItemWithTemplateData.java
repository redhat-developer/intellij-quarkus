package org.acme.qute;

import java.math.BigDecimal;

import io.quarkus.qute.TemplateData;

@TemplateData(target = BigDecimal.class)
@TemplateData(ignoreSuperclasses = true)
public class ItemWithTemplateData {

	public final String name;

	public final BigDecimal price;

	public static String count;

	public ItemWithTemplateData(BigDecimal price, String name) {
		this.price = price;
		this.name = name;
	}

	public Item[] getDerivedItems() {
		return null;
	}

	public static BigDecimal staticMethod(Item item) {
		return item.price.multiply(new BigDecimal("0.9"));
	}
}