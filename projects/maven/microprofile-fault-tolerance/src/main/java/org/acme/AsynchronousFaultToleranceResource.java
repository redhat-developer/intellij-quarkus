/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

package org.acme;

public class AsynchronousFaultTolernaceResource {
	
	@Asynchronous
    public Future<Object> futureAsynchronousMethod() {
        return CompletableFuture.completedFuture(new Object());
    }
	     
    @Asynchronous
    public CompletionStage<Object> completionStageAsynchronousMethod() {
        return CompletableFuture.completedFuture(new Object());
    }
	
	@Asynchronous
    public Object objectReturnTypeAsynchronousMethod() {
    	return new Object();
    }
    
    @Asynchronous
    public void noReturnTypeAsynchronousMethod() {
    	return;
    }
    
    @Asynchronous
    public CompletableFuture<Object> completableFutureAsynchronousMethod() {
    	return CompletableFuture.completedFuture(new Object());
    }
    
}