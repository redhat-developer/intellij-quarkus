package org.acme;

import java.io.IOException;
import java.sql.Connection;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;

@Asynchronous
@Bulkhead
public class MyClient {

	/**
	 * The configured the max retries is 90 but the max duration is 1000ms. Once the
	 * duration is reached, no more retries should be performed, even through it has
	 * not reached the max retries.
	 */
	@Retry(maxRetries = 90, maxDuration = 1000)
	public void serviceA() {
	}

	/**
	 * There should be 0-800ms (jitter is -400ms - 400ms) delays between each
	 * invocation. there should be at least 4 retries but no more than 10 retries.
	 */
	@Retry(delay = 400, maxDuration = 3200, jitter = 400, maxRetries = 10)
	public Connection serviceB() {
		return null;
	}

	/**
	 * Sets retry condition, which means Retry will be performed on IOException.
	 */
	@Retry(retryOn = { IOException.class })
	public void serviceC() {

	}

}