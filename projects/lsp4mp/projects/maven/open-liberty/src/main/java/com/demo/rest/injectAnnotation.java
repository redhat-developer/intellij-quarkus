package com.demo.rest;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.inject.Inject;

@Path("/api/inject")
public class injectAnnotation {

    @Inject
    @RestClient
    public Service NoAnnotationMissing;

    @RestClient
    public Service InjectAnnotationMissing;

    public Service RestClientAndInjectAnnotationMissing;

    @GET
    public String getMy() {
        return "my";
    }

}