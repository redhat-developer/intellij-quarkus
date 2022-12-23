package org.acme.restclient;

import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

public class Fields {
 
	public Country country;

	@Inject                                          
	@RestClient
	public MyService service1, service2;
	
	@Inject
	public CountriesService RestClientAnnotationMissing;
	
	@RestClient
	public CountriesService InjectAnnotationMissing;
	
	public CountriesService RestClientAndInjectAnnotationMissing;
}     
          