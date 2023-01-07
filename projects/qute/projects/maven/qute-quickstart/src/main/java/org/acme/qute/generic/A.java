package org.acme.qute.generic;

import java.util.Iterator;

public class A<A1, A2> extends B<A2, String> implements Iterable<A1> {

	@Override
	public Iterator<A1> iterator() {
		return null;
	}

}
