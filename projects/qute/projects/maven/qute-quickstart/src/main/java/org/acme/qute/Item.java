package org.acme.qute;

import java.math.BigDecimal;

public class Item {

	/**
	 * The name of the item
	 */
	public final String name;

	public final BigDecimal price;

	private final int identifier = 0, version = 1;

	private double volume;

	public Item(BigDecimal price, String name) {
		this.price = price;
		this.name = name;
	}

	/**
	 * Returns the derived items.
	 * 
	 * @return the derived items
	 */
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