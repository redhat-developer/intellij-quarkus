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
package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * LSP progress indicator which wraps the IJ ProgressIndicator and add the cancel support by checking that language client is not disposed.
 */
public class LSPProgressIndicator implements ProgressIndicator {

    private final ProgressIndicator delegate;

    private final LanguageClientImpl languageClient;

    public LSPProgressIndicator(ProgressIndicator delegate, LanguageClientImpl languageClient) {
        this.delegate = delegate;
        this.languageClient = languageClient;
    }


    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }

    @Override
    public void cancel() {
        delegate.cancel();
    }

    @Override
    public boolean isCanceled() {
        return delegate.isCanceled() || languageClient.isDisposed();
    }

    @Override
    public void setText(@NlsContexts.ProgressText String text) {
        delegate.setText(text);
    }

    @Override
    public @NlsContexts.ProgressText String getText() {
        return delegate.getText();
    }

    @Override
    public void setText2(@NlsContexts.ProgressDetails String text) {
        delegate.setText2(text);

    }

    @Override
    public @NlsContexts.ProgressDetails String getText2() {
        return delegate.getText2();
    }

    @Override
    public double getFraction() {
        return delegate.getFraction();
    }

    @Override
    public void setFraction(double fraction) {
        delegate.setFraction(fraction);
    }

    @Override
    public void pushState() {
        delegate.pushState();
    }

    @Override
    public void popState() {
        delegate.popState();
    }

    @Override
    public boolean isModal() {
        return delegate.isModal();
    }

    @Override
    public @NotNull ModalityState getModalityState() {
        return delegate.getModalityState();
    }

    @Override
    public void setModalityProgress(@Nullable ProgressIndicator modalityProgress) {
        delegate.setModalityProgress(modalityProgress);
    }

    @Override
    public boolean isIndeterminate() {
        return delegate.isIndeterminate();
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {
        delegate.setIndeterminate(indeterminate);
    }

    @Override
    public void checkCanceled() throws ProcessCanceledException {
        delegate.checkCanceled();
        if (languageClient.isDisposed()) {
            throw new ProcessCanceledException();
        }
    }

    @Override
    public boolean isPopupWasShown() {
        return delegate.isPopupWasShown();
    }

    @Override
    public boolean isShowing() {
        return delegate.isShowing();
    }
}
