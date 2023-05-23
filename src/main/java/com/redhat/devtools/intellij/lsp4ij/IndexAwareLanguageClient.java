/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class IndexAwareLanguageClient extends LanguageClientImpl {
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexAwareLanguageClient.class);

  public IndexAwareLanguageClient(Project project) {
    super(project);
  }

  protected <R> CompletableFuture<R> runAsBackground(String title, Function<ProgressIndicator, R> function) {
    CompletableFuture<R> future = new CompletableFuture<>();
    CompletableFuture.runAsync(() -> {
      Runnable task = () -> ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), title) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          runTask(indicator, future, function);
        }

        @Override
        public boolean isHeadless() {
          return true;
        }
      });
      if (DumbService.getInstance(getProject()).isDumb()) {
        DumbService.getInstance(getProject()).runWhenSmart(task);
      } else {
        task.run();
      }
    });
    return future;
  }

  private <R> void runTask(@NotNull ProgressIndicator indicator, CompletableFuture<R> future, Function<ProgressIndicator, R> function) {
    boolean done = false;
    for(int i=0; !done && i < 10;++i) {
      try {
        future.complete(function.apply(indicator));
        done = true;
      } catch (IndexNotReadyException e) {
      } catch (Throwable t) {
        future.completeExceptionally(t);
        done = true;
      }
    }
    if (!done) {
      DumbService.getInstance(getProject()).runWhenSmart(() -> runTask(indicator, future, function));
    }
  }
}
