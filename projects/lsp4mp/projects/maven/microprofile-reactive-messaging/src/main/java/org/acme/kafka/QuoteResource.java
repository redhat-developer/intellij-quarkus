package org.acme.kafka;

import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.reactive.messaging.Channel;

public class QuoteResource {

	@Channel("quotes")
    Multi<Quote> quotes;

}
