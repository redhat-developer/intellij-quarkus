package org.acme.qute;

import java.math.BigDecimal;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(fields = false)
public class ItemWithRegisterForReflection {

	public final String name;

	public final BigDecimal price;

	public ItemWithRegisterForReflection(BigDecimal price, String name) {
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