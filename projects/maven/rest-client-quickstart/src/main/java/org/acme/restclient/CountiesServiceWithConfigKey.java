package org.acme.restclient;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "configKey")
public interface CountiesServiceWithConfigKey {

}
