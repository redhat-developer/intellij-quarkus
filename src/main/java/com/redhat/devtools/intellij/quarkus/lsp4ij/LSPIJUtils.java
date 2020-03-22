package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class LSPIJUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(LSPIJUtils.class);

    @Nonnull
    public static List<FileType> getFileContentTypes(@Nonnull VirtualFile file) {
        return Collections.singletonList(file.getFileType());
    }

    private static <T extends TextDocumentPositionParams> T toTextDocumentPositionParamsCommon(T param, int offset, Document document) {
        URI uri = toUri(document);
        Position start = toPosition(offset, document);
        param.setPosition(start);
        TextDocumentIdentifier id = new TextDocumentIdentifier();
        if (uri != null) {
            param.setUri(uri.toString());
            id.setUri(uri.toString());
        }
        param.setTextDocument(id);
        return param;
    }

    public static TextDocumentPositionParams toTextDocumentPosistionParams(int offset, Document document) {
        return toTextDocumentPositionParamsCommon(new TextDocumentPositionParams(), offset, document);
    }


    public static URI toUri(File file) {
        // URI scheme specified by language server protocol and LSP
        try {
            return new URI("file", "", file.getAbsoluteFile().toURI().getPath(), null); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (URISyntaxException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            return file.getAbsoluteFile().toURI();
        }
    }

    public static URI toUri(VirtualFile file) {
        try {
            if (file.getPath().startsWith("/")) {
                return new URI(file.getFileSystem().getProtocol() + ':' + file.getPath());
            } else {
                return new URI(file.getFileSystem().getProtocol() + ":/" + file.getPath());

            }
        } catch (URISyntaxException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            return URI.create(file.getUrl());
        }
    }

    public static URI toUri(Document document) {
        VirtualFile file = getFile(document);
        return file != null ? toUri(file) : null;
    }

    public static VirtualFile getFile(Document document) {
        return FileDocumentManager.getInstance().getFile(document);
    }

    public static Document getDocument(VirtualFile docFile) {
        return FileDocumentManager.getInstance().getDocument(docFile);
    }

    public static Module getProject(VirtualFile file) {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
            if (module != null) {
                return module;
            }
        }
        return null;
    }

    public static int toOffset(Position start, Document document) {
        int lineStartOffset = document.getLineStartOffset(start.getLine());
        return lineStartOffset + start.getCharacter();
    }

    public static Position toPosition(int offset, Document document) {
        int line = document.getLineNumber(offset);
        int lineStart = document.getLineStartOffset(line);
        String lineTextBeforeOffset = document.getText(new TextRange(lineStart, offset));
        int column = lineTextBeforeOffset.length();
        return new Position(line, column);
    }

    @Nonnull
    public static WorkspaceFolder toWorkspaceFolder(@Nonnull Module project) {
        WorkspaceFolder folder = new WorkspaceFolder();
        folder.setUri(toUri(project).toString());
        folder.setName(project.getName());
        return folder;
    }

    public static URI toUri(Module project) {
        File file = new File(project.getModuleFilePath()).getParentFile();
        return file.toURI();
    }

    public static void applyWorkspaceEdit(WorkspaceEdit edit) {
        //TODO: implements WorkspaceEdit
    }

    public static List<FileType> getDocumentContentTypes(Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        return Collections.singletonList(file.getFileType());
    }

    public static VirtualFile findResourceFor(URI uri) {
        return LocalFileSystem.getInstance().findFileByIoFile(Paths.get(uri).toFile());
    }

    public static VirtualFile findResourceFor(String uri) {
        return LocalFileSystem.getInstance().findFileByIoFile(Paths.get(uri).toFile());
    }

    public static Editor[] editorsForFile(VirtualFile file) {
        Editor[] editors = new Editor[0];
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            editors = editorsForFile(file, document);
        }
        return editors;
    }

    public static Editor[] editorsForFile(VirtualFile file, Document document) {
        return EditorFactory.getInstance().getEditors(document, LSPIJUtils.getProject(file).getProject());
    }

    public static Editor editorForFile(VirtualFile file) {
        Editor[] editors = editorsForFile(file);
        return editors.length > 0 ? editors[0] : null;
    }


    public static CompletionParams toCompletionParams(URI fileUri, int offset, Document document) {
        Position start = toPosition(offset, document);
        CompletionParams param = new CompletionParams();
        param.setPosition(start);
        param.setUri(fileUri.toString());
        TextDocumentIdentifier id = new TextDocumentIdentifier();
        id.setUri(fileUri.toString());
        param.setTextDocument(id);
        return param;
    }

    public static void applyEdit(Editor editor, TextEdit textEdit, Document document) {
        RangeMarker marker = document.createRangeMarker(LSPIJUtils.toOffset(textEdit.getRange().getStart(), document), LSPIJUtils.toOffset(textEdit.getRange().getEnd(), document));
        int startOffset = marker.getStartOffset();
        int endOffset = marker.getEndOffset();
        String text = textEdit.getNewText();
        if (text == null || "".equals(text)) {
            document.deleteString(startOffset, endOffset);
        } else if (endOffset - startOffset <= 0) {
            document.insertString(startOffset, text);
        } else {
            document.replaceString(startOffset, endOffset, text);
        }
        if (text != null && !"".equals(text)) {
            editor.getCaretModel().moveCaretRelatively(text.length(), 0, false, false, true);
        }
        marker.dispose();
    }


    public static void applyEdits(Editor editor, Document document, List<TextEdit> edits) {
        ApplicationManager.getApplication().runWriteAction(() -> edits.forEach(edit -> applyEdit(editor, edit, document)));
    }
}
