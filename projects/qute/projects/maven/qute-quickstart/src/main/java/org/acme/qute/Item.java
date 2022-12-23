package org.acme.qute;

import java.math.BigDecimal;

public class Item {

	public final String name;

	public final BigDecimal price;

	public Item(BigDecimal price, String name) {
		this.price = price;
		this.name = name;
	}

	public Item[] getDerivedItems() {
		return null;
	}

	public String varArgsMethod(int index, String... elements) {
		return null;
	}
	
	public static BigDecimal staticMethod(Item item) {
		return item.price.multiply(new BigDecimal("0.9"));
	}

}