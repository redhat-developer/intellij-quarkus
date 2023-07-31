/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.projectWizard;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Shows a modal Progress dialog while waiting for a {@link Future} to complete
 */
public class RequestHelper {

    private RequestHelper() {
    }

    /**
     * Shows a modal Progress dialog while waiting for a {@link Future} to complete.
     * @param request the request to wait for
     * @param title the title of the progress bar
     * @param durationInMs the maximum duration in milliseconds to keep the progress dialog open
     * @param project the {@link Project} this dialog applies to
     */
    public static void waitFor(@NotNull Future<?> request, @NotNull String title, int durationInMs,  Project project) {
        if (isComplete(request)) {
            return;
        }
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            //Let's wait for DEFAULT_TIMEOUT sec at most (request should time out before that)
            ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            // Wait for the loading to finish
            for (int i = 0; i < durationInMs; i++) {
                indicator.checkCanceled();
                if (isComplete(request)) {
                    return;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    throw new ProcessCanceledException(e);
                }
            }
        }, title, true, project);
    }

    private static boolean isComplete(Future<?> future){
        return future.isDone() || future.isCancelled();
    }
}
