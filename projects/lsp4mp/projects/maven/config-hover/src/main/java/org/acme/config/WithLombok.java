package org.acme.config;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class WithLombok {

	public String myField;
	
	WithLombok(String myField) {
		this.myField = myField;
	}
	
}
