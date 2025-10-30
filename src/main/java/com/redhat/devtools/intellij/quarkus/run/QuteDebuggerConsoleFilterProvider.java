// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.redhat.devtools.intellij.quarkus.run;

import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.codeInsight.hints.presentation.PresentationRenderer;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.actions.JavaDebuggerActionsCollector;
import com.intellij.debugger.impl.attach.JavaAttachDebuggerProvider;
import com.intellij.execution.filters.ConsoleFilterProvider;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.impl.InlayProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuteDebuggerConsoleFilterProvider implements ConsoleFilterProvider {

  @Override
  public Filter @NotNull [] getDefaultFilters(@NotNull Project project) {
    return new Filter[]{new QuteDebuggerAttachFilter(project)};
  }

  public static @Nullable Integer getConnectionMatcher(String line) {
    if (line.contains("Qute debugger server listening on port ")) {
      String port = line.substring("Qute debugger server listening on port ".length()).trim();
      return Integer.parseInt(port);
    }
    return null;
  }

  private static class QuteDebuggerAttachFilter implements Filter {
    @NotNull Project myProject;

    private QuteDebuggerAttachFilter(@NotNull Project project) {
      this.myProject = project;
    }

    @Override
    public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
      Integer port = getConnectionMatcher(line);
      if (port == null) {
          return null;
      }

      if (Registry.is("debugger.auto.attach.from.any.console") && !isDebuggerAttached(port, myProject)) {
        ApplicationManager.getApplication().invokeLater(
          () -> AttachDebuggerProcessListener.createQuteConfiguration(port, "", myProject, new EmptyProgressIndicator(), true),//JavaAttachDebuggerProvider.attach(transport, address, null, myProject),
          ModalityState.any());
      }

      int start = entireLength - line.length();
      // to trick the code unwrapping single results in com.intellij.execution.filters.CompositeFilter#createFinalResult
      return new Result(Arrays.asList(
        new AttachInlayResult(start, start + line.length() - 1, port),
        new ResultItem(0, 0, null)));
    }
  }

  private static boolean isDebuggerAttached(Integer port, Project project) {
    /*return DebuggerManagerEx.getInstanceEx(project).getSessions()
      .stream()
      .map(s -> s.getDebugEnvironment().getRemoteConnection())
      .anyMatch(c -> address.equals(c.getApplicationAddress()) && "dt_shmem".equals(transport) != c.isUseSockets());

     */
      return false;
  }

  private static class AttachInlayResult extends Filter.ResultItem implements InlayProvider {
    private final Integer port;

    AttachInlayResult(int highlightStartOffset, int highlightEndOffset, Integer port) {
      super(highlightStartOffset, highlightEndOffset, null);
      this.port = port;
    }

    @Override
    public EditorCustomElementRenderer createInlayRenderer(Editor editor) {
      PresentationFactory factory = new PresentationFactory(editor);
      InlayPresentation presentation = factory.referenceOnHover(
        factory.roundWithBackground(factory.smallText("Qute Attach debugger")),
        (event, point) -> {
            AttachDebuggerProcessListener.createQuteConfiguration(port, "", editor.getProject(), new EmptyProgressIndicator(), true);
          //JavaDebuggerActionsCollector.attachFromConsoleInlay.log();
          //JavaAttachDebuggerProvider.attach(myTransport, myAddress, null, editor.getProject());
        });
      return new PresentationRenderer(presentation);
    }
  }
}