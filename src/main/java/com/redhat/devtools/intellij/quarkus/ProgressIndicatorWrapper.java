/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Progress indicator wrapper.
 */
public class ProgressIndicatorWrapper implements ProgressIndicator {

    private final ProgressIndicator progressIndicator;

    public ProgressIndicatorWrapper(ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;

    }
    public void start() {
        progressIndicator.start();
    }

    public void stop() {
        progressIndicator.stop();
    }

    public boolean isRunning() {
        return progressIndicator.isRunning();
    }

    public void cancel() {
        progressIndicator.cancel();
    }

    public boolean isCanceled() {
        return progressIndicator.isCanceled();
    }

    public void setText(@NlsContexts.ProgressText String text) {
        progressIndicator.setText(text);
    }

    @NlsContexts.ProgressText
    public String getText() {
        return progressIndicator.getText();
    }

    public void setText2(@NlsContexts.ProgressDetails String text) {
        progressIndicator.setText2(text);
    }

    @NlsContexts.ProgressDetails
    public String getText2() {
        return progressIndicator.getText2();
    }

    public double getFraction() {
        return progressIndicator.getFraction();
    }

    public void setFraction(double fraction) {
        progressIndicator.setFraction(fraction);
    }

    public void pushState() {
        progressIndicator.pushState();
    }

    public void popState() {
        progressIndicator.popState();
    }

    public boolean isModal() {
        return progressIndicator.isModal();
    }

    public @NotNull ModalityState getModalityState() {
        return progressIndicator.getModalityState();
    }

    public void setModalityProgress(@Nullable ProgressIndicator modalityProgress) {
        progressIndicator.setModalityProgress(modalityProgress);
    }

    public boolean isIndeterminate() {
        return progressIndicator.isIndeterminate();
    }

    public void setIndeterminate(boolean indeterminate) {
        progressIndicator.setIndeterminate(indeterminate);
    }

    public void checkCanceled() throws ProcessCanceledException {
        progressIndicator.checkCanceled();
        if (isCanceled() /*&& isCancelable()*/) {
            Throwable trace = getCancellationTrace();
            throw trace instanceof ProcessCanceledException ? (ProcessCanceledException)trace : new ProcessCanceledException(trace);
        }
    }

    protected @Nullable Throwable getCancellationTrace() {
        return this instanceof Disposable ? Disposer.getDisposalTrace((Disposable)this) : null;
    }

    public boolean isPopupWasShown() {
        return progressIndicator.isPopupWasShown();
    }

    public boolean isShowing() {
        return progressIndicator.isShowing();
    }

}
