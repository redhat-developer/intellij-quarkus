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
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.PublishDiagnosticsParams;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.awt.Font;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Consumer;

public class LSPDiagnosticsToMarkers implements Consumer<PublishDiagnosticsParams> {
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
                e.printStackTrace();
            }
            if (file != null) {
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document != null) {
                    Editor[] editors  = LSPIJUtils.editorsForFile(file, document);
                    for(Editor editor : editors) {
                        cleanMarkers(editor.getMarkupModel());
                        createMarkers(editor, document, publishDiagnosticsParams.getDiagnostics());
                    }
                }
            }
        });
    }

    private void createMarkers(Editor editor, Document document, List<Diagnostic> diagnostics) {
        for(Diagnostic diagnostic : diagnostics) {
            int startOffset = LSPIJUtils.toOffset(diagnostic.getRange().getStart(), document);
            int endOffset = LSPIJUtils.toOffset(diagnostic.getRange().getEnd(), document);
            int layer = getLayer(diagnostic.getSeverity());
            EffectType effectType = getEffectType(diagnostic.getSeverity());
            Color color = getColor(diagnostic.getSeverity());
            editor.getMarkupModel().addRangeHighlighter(startOffset, endOffset, layer, new TextAttributes(editor.getColorsScheme().getDefaultForeground(), editor.getColorsScheme().getDefaultBackground(), color, effectType, Font.PLAIN), HighlighterTargetArea.EXACT_RANGE);
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

    private void cleanMarkers(MarkupModel markupModel) {
        RangeHighlighter[] highlighters = markupModel.getAllHighlighters();
        for(RangeHighlighter highlighter : highlighters) {
            if (belongsToServer(highlighter, languageServerId)) {
                markupModel.removeHighlighter(highlighter);
            }
        }
    }

    private boolean belongsToServer(RangeHighlighter highlighter, String languageServerId) {
        Key<Boolean> key = new Key<>(LSPDiagnosticsToMarkers.class.getName() + '.' + languageServerId);
         return highlighter.getUserData(key) != null;
    }
}
