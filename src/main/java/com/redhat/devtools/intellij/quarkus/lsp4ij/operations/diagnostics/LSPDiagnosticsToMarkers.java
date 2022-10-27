package com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.awt.Font;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class LSPDiagnosticsToMarkers implements Consumer<PublishDiagnosticsParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LSPDiagnosticsToMarkers.class);

    private static final Key<Map<String, RangeHighlighter[]>> LSP_MARKER_KEY_PREFIX = Key.create(LSPDiagnosticsToMarkers.class.getName() + ".markers");

    private final String languageServerId;

    public LSPDiagnosticsToMarkers(@Nonnull String serverId) {
        this.languageServerId = serverId;
    }
    @Override
    public void accept(PublishDiagnosticsParams publishDiagnosticsParams) {
        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile file = null;
            try {
                file = LSPIJUtils.findResourceFor(new URI(publishDiagnosticsParams.getUri()));
            } catch (URISyntaxException e) {
                LOGGER.warn(e.getLocalizedMessage(), e);
            }
            if (file != null) {
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document != null) {
                    Editor[] editors  = LSPIJUtils.editorsForFile(file, document);
                    for(Editor editor : editors) {
                        cleanMarkers(editor);
                        createMarkers(editor, document, publishDiagnosticsParams.getDiagnostics());
                    }
                }
            }
        });
    }

    private void createMarkers(Editor editor, Document document, List<Diagnostic> diagnostics) {
        RangeHighlighter[] rangeHighlighters = new RangeHighlighter[diagnostics.size()];
        int index = 0;
        for(Diagnostic diagnostic : diagnostics) {
            int startOffset = LSPIJUtils.toOffset(diagnostic.getRange().getStart(), document);
            int endOffset = LSPIJUtils.toOffset(diagnostic.getRange().getEnd(), document);
            if (endOffset > document.getLineEndOffset(document.getLineCount() - 1)) {
                endOffset = document.getLineEndOffset(document.getLineCount() - 1);
            }
            int layer = getLayer(diagnostic.getSeverity());
            EffectType effectType = getEffectType(diagnostic.getSeverity());
            Color color = getColor(diagnostic.getSeverity());
            RangeHighlighter rangeHighlighter = editor.getMarkupModel().addRangeHighlighter(startOffset, endOffset, layer, new TextAttributes(editor.getColorsScheme().getDefaultForeground(), editor.getColorsScheme().getDefaultBackground(), color, effectType, Font.PLAIN), HighlighterTargetArea.EXACT_RANGE);
            rangeHighlighter.setErrorStripeTooltip(diagnostic);
            rangeHighlighters[index++] = rangeHighlighter;
        }
        Map<String, RangeHighlighter[]> allMarkers = getAllMarkers(editor);
        allMarkers.put(languageServerId, rangeHighlighters);

    }

    @NotNull
    private Map<String, RangeHighlighter[]> getAllMarkers(Editor editor) {
        if (editor instanceof UserDataHolderBase) {
            return ((UserDataHolderBase) editor).putUserDataIfAbsent(LSP_MARKER_KEY_PREFIX, new HashMap<>());
        } else {
            synchronized (editor) {
                Map<String, RangeHighlighter[]> allMarkers = editor.getUserData(LSP_MARKER_KEY_PREFIX);
                if (allMarkers == null) {
                    allMarkers = new HashMap<>();
                    editor.putUserData(LSP_MARKER_KEY_PREFIX, allMarkers);
                }
                return allMarkers;
            }
        }
    }

    private EffectType getEffectType(DiagnosticSeverity severity) {
        return severity== DiagnosticSeverity.Hint?EffectType.BOLD_DOTTED_LINE:EffectType.WAVE_UNDERSCORE;
    }

    private int getLayer(DiagnosticSeverity severity) {
        return severity== DiagnosticSeverity.Error?HighlighterLayer.ERROR:HighlighterLayer.WARNING;
    }

    private Color getColor(DiagnosticSeverity severity) {
        switch (severity) {
            case Hint:
                return Color.GRAY;
            case Error:
                return Color.RED;
            case Information:
                return Color.GRAY;
            case Warning:
                return Color.YELLOW;
        }
        return Color.GRAY;
    }

    private void cleanMarkers(Editor editor) {
        Map<String, RangeHighlighter[]> allMarkers = getAllMarkers(editor);
        RangeHighlighter[] highlighters = allMarkers.get(languageServerId);
        MarkupModel markupModel = editor.getMarkupModel();
        if (highlighters != null) {
            for (RangeHighlighter highlighter : highlighters) {
                markupModel.removeHighlighter(highlighter);
            }
        }
        allMarkers.remove(languageServerId);
    }


    public static RangeHighlighter[] getMarkers(Editor editor, String languageServerId) {
        Map<String, RangeHighlighter[]> allMarkers = editor.getUserData(LSP_MARKER_KEY_PREFIX);
        return allMarkers!=null?allMarkers.get(languageServerId):null;
    }
}
