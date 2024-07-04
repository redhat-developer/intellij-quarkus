package org.acme;

public class Bean3 {

    // Quarkus can inject Bean1 in the constructor without declaring the bean1 parameter with @Inject
    public Bean3(Bean1 bean1) {

    }
}
