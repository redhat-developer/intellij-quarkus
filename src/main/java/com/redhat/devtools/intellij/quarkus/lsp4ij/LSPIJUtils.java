package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.apache.commons.lang.StringUtils;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CreateFile;
import org.eclipse.lsp4j.DeleteFile;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.RenameFile;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LSPIJUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(LSPIJUtils.class);

    @Nonnull
    public static Language getFileLanguage(@Nonnull VirtualFile file, Project project) {
        return ReadAction.compute(() -> LanguageUtil.getLanguageForPsi(project, file));
    }

    private static <T extends TextDocumentPositionParams> T toTextDocumentPositionParamsCommon(T param, int offset, Document document) {
        URI uri = toUri(document);
        Position start = toPosition(offset, document);
        param.setPosition(start);
        TextDocumentIdentifier id = new TextDocumentIdentifier();
        if (uri != null) {
            id.setUri(uri.toString());
        }
        param.setTextDocument(id);
        return param;
    }

    public static TextDocumentPositionParams toTextDocumentPosistionParams(int offset, Document document) {
        return toTextDocumentPositionParamsCommon(new TextDocumentPositionParams(), offset, document);
    }

    public static HoverParams toHoverParams(int offset, Document document) {
        return toTextDocumentPositionParamsCommon(new HoverParams(), offset, document);
    }

    public static URI toUri(File file) {
        // URI scheme specified by language server protocol and LSP
        try {
            return new URI("file", "", file.getAbsoluteFile().toURI().getPath(), null); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (URISyntaxException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
            return file.getAbsoluteFile().toURI();
        }
    }

    public static URI toUri(VirtualFile file) {
        return toUri(VfsUtilCore.virtualToIoFile(file));
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
            Module module = ReadAction.compute(() -> ProjectFileIndex.getInstance(project).getModuleForFile(file));
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
        applyWorkspaceEdit(edit, null);
    }

    public static void applyWorkspaceEdit(WorkspaceEdit edit, String label) {
        if (edit.getDocumentChanges() != null) {
            for(Either<TextDocumentEdit, ResourceOperation> change : edit.getDocumentChanges()) {
                if (change.isLeft()) {
                    VirtualFile file = findResourceFor(change.getLeft().getTextDocument().getUri());
                    if (file != null) {
                        Document document = getDocument(file);
                        if (document != null) {
                            applyWorkspaceEdit(document, change.getLeft().getEdits());
                        }
                    }
                } else if (change.isRight()) {
                    ResourceOperation resourceOperation = change.getRight();
                    if (resourceOperation instanceof CreateFile) {
                        try {
                            CreateFile createOperation = (CreateFile) resourceOperation;
                            URI targetURI = URI.create(createOperation.getUri());
                            VirtualFile targetFile = VfsUtil.findFileByURL(targetURI.toURL());
                            if (targetFile != null && createOperation.getOptions() != null) {
                                if (!createOperation.getOptions().getIgnoreIfExists()) {
                                    Document document = getDocument(targetFile);
                                    if (document != null) {
                                        TextEdit textEdit = new TextEdit(new Range(toPosition(0, document), toPosition(document.getTextLength(), document)), "");
                                        applyWorkspaceEdit(document, Collections.singletonList(textEdit));
                                    }
                                }
                            } else {
                                try {
                                    File f = new File(targetURI);
                                    f.createNewFile();
                                    VfsUtil.findFileByIoFile(f, true);
                                } catch (IOException e) {
                                    LOGGER.warn(e.getLocalizedMessage(), e);
                                }
                            }
                        } catch (MalformedURLException e) {
                            LOGGER.warn(e.getLocalizedMessage(), e);
                        }
                    } else if (resourceOperation instanceof DeleteFile) {
                        try {
                            VirtualFile resource = findResourceFor(((DeleteFile) resourceOperation).getUri());
                            if (resource != null) {
                                resource.delete(null);
                            }
                        } catch (IOException e) {
                            LOGGER.warn(e.getLocalizedMessage(), e);
                        }
                    }
                }
            }
        } else if (edit.getChanges() != null) {
            for(Map.Entry<String, List<TextEdit>> change : edit.getChanges().entrySet()) {
                VirtualFile file = findResourceFor(change.getKey());
                if (file != null) {
                    Document document = getDocument(file);
                    if (document != null) {
                        applyWorkspaceEdit(document, change.getValue());
                    }
                }

            }

        }
    }

    private static void applyWorkspaceEdit(Document document, List<TextEdit> edits) {
        for(TextEdit edit : edits) {
            if (edit.getRange() != null) {
                String text = edit.getNewText();
                int start = toOffset(edit.getRange().getStart(), document);
                int end = toOffset(edit.getRange().getEnd(), document);
                if (StringUtils.isEmpty(text)) {
                    document.deleteString(start, end);
                } else {
                    text = text.replaceAll("\r", "");
                    if (end >= 0) {
                        if (end - start <= 0) {
                            document.insertString(start, text);
                        } else {
                            document.replaceString(start, end, text);
                        }
                    } else if (start == 0) {
                        document.setText(text);
                    } else if (start > 0) {
                        document.insertString(start, text);
                    }
                }
            }
        }
    }


    public static Language getDocumentLanguage(Document document, Project project) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        return getFileLanguage(file, project);
    }

    public static VirtualFile findResourceFor(URI uri) {
        return LocalFileSystem.getInstance().findFileByIoFile(Paths.get(uri).toFile());
    }

    public static VirtualFile findResourceFor(String uri) {
        try {
            return VfsUtil.findFileByURL(new URL(uri));
        } catch (MalformedURLException e) {
            return null;
        }
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
        Module module = LSPIJUtils.getProject(file);
        return module!=null?EditorFactory.getInstance().getEditors(document, module.getProject()):new Editor[0];
    }

    public static Editor editorForFile(VirtualFile file) {
        Editor[] editors = editorsForFile(file);
        return editors.length > 0 ? editors[0] : null;
    }

    public static Editor editorForElement(PsiElement element) {
        if (element.getContainingFile() != null && element.getContainingFile().getVirtualFile() != null) {
            return editorForFile(element.getContainingFile().getVirtualFile());
        }
        return null;
    }

    public static CompletionParams toCompletionParams(URI fileUri, int offset, Document document) {
        Position start = toPosition(offset, document);
        CompletionParams param = new CompletionParams();
        param.setPosition(start);
        param.setTextDocument(toTextDocumentIdentifier(fileUri));
        return param;
    }

    public static TextDocumentIdentifier toTextDocumentIdentifier(final URI uri) {
        return new TextDocumentIdentifier(uri.toASCIIString());
    }

    public static void applyEdit(Editor editor, TextEdit textEdit, Document document) {
        RangeMarker marker = document.createRangeMarker(LSPIJUtils.toOffset(textEdit.getRange().getStart(), document), LSPIJUtils.toOffset(textEdit.getRange().getEnd(), document));
        int startOffset = marker.getStartOffset();
        int endOffset = marker.getEndOffset();
        String text = textEdit.getNewText();
        if (text != null) {
            text = text.replaceAll("\r", "");
        }
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

    public static boolean hasCapability(final Either<Boolean, ? extends Object> eitherCapability) {
        if(eitherCapability == null) {
            return false;
        }
        return eitherCapability.isRight() || (eitherCapability.isLeft() && eitherCapability.getLeft());
    }
}
