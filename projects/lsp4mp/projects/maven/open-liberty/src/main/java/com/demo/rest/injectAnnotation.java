package com.demo.rest;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.inject.Inject;

public class injectAnnotation {

    @Inject
    @RestClient
    public MyService NoAnnotationMissing;

    @RestClient
    public Service InjectAnnotationMissing;

    public Service RestClientAndInjectAnnotationMissing;

}