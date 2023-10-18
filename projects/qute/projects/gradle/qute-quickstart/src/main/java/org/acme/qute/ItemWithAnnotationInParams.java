package org.acme.qute;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ItemWithAnnotationInParams {

	public Item getItemByIndex(@NotNull Item item, @NotNull int index) {
		return null;
	}
}