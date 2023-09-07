package io.openliberty.graphql.sample;

import io.smallrye.graphql.api.Directive;
import io.smallrye.graphql.api.DirectiveLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Directive(on = {DirectiveLocation.ENUM})
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Optimistic {
}
