/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4ij.client;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.redhat.devtools.intellij.lsp4ij.LanguageServersRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.CancellablePromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * LSP completable future which execute a given function code in a non blocking reading action promise.
 */
public class LSPCompletableFuture<R> extends CompletableFuture<R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSPCompletableFuture.class);

    private static final int MAX_ATTEMPT = 5;
    private final Function<ProgressIndicator, R> code;
    private final IndexAwareLanguageClient languageClient;
    private final String progressTitle;
    private final AtomicInteger nbAttempt;
    private CancellablePromise<R> nonBlockingReadActionPromise;

    public LSPCompletableFuture(Function<ProgressIndicator, R> code, String progressTitle, IndexAwareLanguageClient languageClient) {
        this.code = code;
        this.progressTitle = progressTitle;
        this.languageClient = languageClient;
        this.nbAttempt = new AtomicInteger(0);
        // if indexation is processing, we need to execute the promise in smart mode
        var executeInmartMode = DumbService.getInstance(languageClient.getProject()).isDumb();
        var promise = nonBlockingReadActionPromise( executeInmartMode);
        bind(promise);
    }

    /**
     * Bind the given promise with the completable future.
     *
     * @param promise the promise which will execute the function code in a non blocking read action context
     */
    private void bind(CancellablePromise<R> promise) {
        this.nonBlockingReadActionPromise = promise;
        // On error...
        promise.onError(ex -> {
            if (ex instanceof IndexNotReadyException || ex instanceof ReadAction.CannotReadException) {
                // Case 1: Attempt to retry the start of the promise
                if (nbAttempt.incrementAndGet() >= MAX_ATTEMPT) {
                    // 1.1 Maximum number reached, mark the completable future as error
                    LOGGER.warn("Maximum number (" +  MAX_ATTEMPT + ")" + " of attempts to start non blocking read action for '" + progressTitle + "' has been reached", ex);
                    this.completeExceptionally(new ExecutionAttemptLimitReachedException(progressTitle, MAX_ATTEMPT, ex));
                } else {
                    // Retry ...
                    // 1.2 Index are not ready or the read action cannot be done, retry in smart mode...
                    LOGGER.warn("Restart non blocking read action for '" + progressTitle + "' with attempt " + nbAttempt.get() + "/" + MAX_ATTEMPT + ".", ex);
                    var newPromise = nonBlockingReadActionPromise(true);
                    bind(newPromise);
                }
            } else if (ex instanceof ProcessCanceledException || ex instanceof CancellationException) {
                // Case 2: cancel the completable future
                this.cancel(true);
            } else {
                // Other case..., mark the completable future as error
                this.completeExceptionally(ex);
            }
        });
        // On success...
        promise.onSuccess(value -> this.complete(value));
    }

    /**
     * Create a non blocking read action promise.
     *
     * @param executeInSmartMode true if the promise must be executed in smart mode and false otherwise.
     *
     * @return a non blocking read action promise
     */
    @NotNull
    private CancellablePromise<R> nonBlockingReadActionPromise(boolean  executeInSmartMode ) {
        var project = languageClient.getProject();

        var indicator = new LSPProgressIndicator(languageClient);
        indicator.setText(progressTitle);
        var action = ReadAction.nonBlocking(() -> code.apply(indicator))
                .wrapProgress(indicator)
                .expireWith(languageClient); // promise is canceled when language client is stopped
        if ( executeInSmartMode ) {
            action = action.inSmartMode(project);
        }
        return action
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (nonBlockingReadActionPromise != null) {
            // cancel the current promise
            nonBlockingReadActionPromise.cancel(mayInterruptIfRunning);
        }
        return super.cancel(mayInterruptIfRunning);
    }

}
