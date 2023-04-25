package com.demo.rest;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@RegisterRestClient
@ApplicationScoped
public interface Service {

    @GET
    @Path("/{parameter}")
    String doSomething(@PathParam("parameter") String parameter);

}