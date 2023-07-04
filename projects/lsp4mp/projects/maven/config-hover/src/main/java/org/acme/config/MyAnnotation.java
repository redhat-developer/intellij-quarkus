package org.acme.config;

public @interface MyAnnotation {

    public static String MY_STRING = "asdf";

    public String value() default MY_STRING;

}
