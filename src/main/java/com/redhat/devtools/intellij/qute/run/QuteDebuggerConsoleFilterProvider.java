// Copyright 2000-2025 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license.

package com.redhat.devtools.intellij.qute.run;

import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.codeInsight.hints.presentation.PresentationRenderer;
import com.intellij.execution.filters.ConsoleFilterProvider;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.impl.InlayProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static com.redhat.devtools.intellij.quarkus.run.AttachDebuggerProcessListener.createQuteConfiguration;
import static com.redhat.devtools.intellij.quarkus.run.AttachDebuggerProcessListener.isDebuggerAutoAttach;

/**
 * QuteDebuggerConsoleFilterProvider
 * <p>
 * Copy-pasted and adapted from IntelliJ Community's JavaDebuggerConsoleFilterProvider
 * for the Qute debugger.
 * <p>
 * Features:
 * 1. Detects console lines indicating that the Qute debugger server is listening on a port.
 * 2. Provides a clickable inlay "Attach Qute debugger" to attach the debugger automatically.
 * 3. Can trigger automatic attachment if the registry key
 * 'debugger.auto.attach.from.any.console' is enabled.
 *
 * @see <a href="https://github.com/JetBrains/intellij-community/blob/master/java/execution/impl/src/com/intellij/execution/impl/JavaDebuggerConsoleFilterProvider.java">JavaDebuggerConsoleFilterProvider.java</a>
 */
public final class QuteDebuggerConsoleFilterProvider implements ConsoleFilterProvider {

    /**
     * Provides default filters for this project.
     */
    @Override
    public Filter @NotNull [] getDefaultFilters(@NotNull Project project) {
        return new Filter[]{new QuteDebuggerAttachFilter(project)};
    }

    /**
     * Detects the Qute server port from a console line.
     * Example line: "Qute debugger server listening on port 5005"
     *
     * @param line a console line
     * @return the detected port or null if the line does not match
     */
    public static @Nullable Integer getConnectionMatcher(String line) {
        if (line.contains("Qute debugger server listening on port ")) {
            String port = line.substring("Qute debugger server listening on port ".length()).trim();
            try {
                return Integer.parseInt(port);
            } catch (Exception e) {
                // Should never occur
                return null;
            }
        }
        return null;
    }

    /**
     * Filter applied to each console line to detect the Qute port and create an inlay.
     */
    private static class QuteDebuggerAttachFilter implements Filter {
        @NotNull Project myProject;

        private QuteDebuggerAttachFilter(@NotNull Project project) {
            this.myProject = project;
        }

        @Override
        public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
            Integer port = getConnectionMatcher(line);
            if (port == null) {
                return null; // No port detected → nothing to do
            }

            // Automatic attachment if registry is enabled and debugger not already attached
            if (isDebuggerAutoAttach()) {
                ApplicationManager.getApplication().invokeLater(
                        () -> createQuteConfiguration(port, "", myProject, new EmptyProgressIndicator(), true),
                        ModalityState.any());
            }

            int start = entireLength - line.length();

            // Return a Result with:
            // 1) an AttachInlayResult for the clickable inlay
            // 2) an empty ResultItem to "trick" CompositeFilter into proper behavior
            return new Result(Arrays.asList(
                    new AttachInlayResult(start, start + line.length() - 1, port),
                    new ResultItem(0, 0, null)
            ));
        }
    }

    /**
     * Custom ResultItem that creates a clickable inlay for attaching the Qute debugger.
     */
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
                    factory.roundWithBackground(factory.smallText("Attach Qute debugger")),
                    (event, point) -> {
                        // On click → attach Qute debugger
                        ProgressManager.getInstance().run(new Task.Backgroundable(editor.getProject(), "Attaching to Qute debugger", true) {
                            @Override
                            public void run(@NotNull ProgressIndicator indicator) {
                                createQuteConfiguration(port, editor.getProject().getName(), editor.getProject(), indicator, true);
                            }
                        });
                    });
            return new PresentationRenderer(presentation);
        }
    }
}
