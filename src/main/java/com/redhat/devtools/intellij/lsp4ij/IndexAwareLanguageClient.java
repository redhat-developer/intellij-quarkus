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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
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

  /**
   * Run the given function as a background task, wrapped in a read action
   * @param title the title of the action being run
   * @param function the function to execute in the background
   * @return the output of the function
   * @param <R> the return type
   */
  protected <R> CompletableFuture<R> runAsBackground(String title, Function<ProgressIndicator, R> function) {
    return runAsBackground(title, function, true);
  }

  /**
   * Run the given function as a background task, which can be wrapped in a read action
   * @param title the title of the action being run
   * @param function the function to execute in the background
   * @param inReadAction whether to wrap the function call in a read action or not
   * @return the output of the function
   * @param <R> the return type
   */
  protected <R> CompletableFuture<R> runAsBackground(String title, Function<ProgressIndicator, R> function, boolean inReadAction) {
    CompletableFuture<R> future = new CompletableFuture<>();
    CompletableFuture.runAsync(() -> {
      Runnable task = () -> ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), title) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          runTask(indicator, future, function, inReadAction);
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

  private <R> void runTask(@NotNull ProgressIndicator indicator, CompletableFuture<R> future, Function<ProgressIndicator, R> function, boolean inReadAction) {
    boolean done = false;
    for(int i=0; !done && i < 10;++i) {
      try {
        R result;
        if (inReadAction) {
          result = ApplicationManager.getApplication().runReadAction((Computable<R>)() -> function.apply(indicator));
        } else {
          result = function.apply(indicator);
        }
        future.complete(result);
        done = true;
      } catch (IndexNotReadyException ignored) {
      } catch (Throwable t) {
        future.completeExceptionally(t);
        done = true;
      }
    }
    if (!done) {
      DumbService.getInstance(getProject()).runWhenSmart(() -> runTask(indicator, future, function, inReadAction));
    }
  }
}
