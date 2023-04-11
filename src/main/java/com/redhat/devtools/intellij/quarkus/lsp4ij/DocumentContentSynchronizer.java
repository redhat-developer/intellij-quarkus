package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DocumentContentSynchronizer implements DocumentListener {
    private final static Logger LOGGER = LoggerFactory.getLogger(DocumentContentSynchronizer.class);

    private final @Nonnull
    LanguageServerWrapper languageServerWrapper;
    private final @Nonnull
    Document document;
    private final @Nonnull
    URI fileUri;
    private final TextDocumentSyncKind syncKind;

    private int version = 0;
    private final List<TextDocumentContentChangeEvent> changeEvents;
    private long modificationStamp;
    final @Nonnull
    CompletableFuture<Void> didOpenFuture;

    public DocumentContentSynchronizer(@Nonnull LanguageServerWrapper languageServerWrapper,
                                       @Nonnull Document document,
                                       TextDocumentSyncKind syncKind) {
        this.languageServerWrapper = languageServerWrapper;
        this.fileUri = LSPIJUtils.toUri(document);
        this.modificationStamp = -1;
        this.syncKind = syncKind != null ? syncKind : TextDocumentSyncKind.Full;

        this.document = document;
        // add a document buffer
        TextDocumentItem textDocument = new TextDocumentItem();
        textDocument.setUri(fileUri.toString());
        textDocument.setText(document.getText());

        Language contentTypes = LSPIJUtils.getDocumentLanguage(this.document, languageServerWrapper.getProject());

        String languageId = languageServerWrapper.getLanguageId(contentTypes);

        //TODO: determine languageId more precisely
        /*IPath fromPortableString = Path.fromPortableString(this.fileUri.getPath());
        if (languageId == null) {
            languageId = fromPortableString.getFileExtension();
            if (languageId == null) {
                languageId = fromPortableString.lastSegment();
            }
        }*/

        textDocument.setLanguageId(languageId);
        textDocument.setVersion(++version);
        didOpenFuture = languageServerWrapper.getInitializedServer()
                .thenAcceptAsync(ls -> ls.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(textDocument)));

        // Initialize LSP change events
        changeEvents = new ArrayList<>();
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        if (syncKind == TextDocumentSyncKind.None) {
            return;
        }
        checkEvent(event);
        if (syncKind == TextDocumentSyncKind.Full) {
            synchronized (changeEvents) {
                changeEvents.clear();
                changeEvents.add(createChangeEvent(event));
            }
        }

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            sendDidChangeEvents();
        } else {
            PsiDocumentManager.getInstance(languageServerWrapper.getProject()).performForCommittedDocument(event.getDocument(), this::sendDidChangeEvents);
        }
    }

    private void sendDidChangeEvents() {
        List<TextDocumentContentChangeEvent> events = null;
        synchronized (changeEvents) {
            events = new ArrayList<>(changeEvents);
            changeEvents.clear();
        }

        DidChangeTextDocumentParams changeParamsToSend = new DidChangeTextDocumentParams(new VersionedTextDocumentIdentifier(), events);
        changeParamsToSend.getTextDocument().setUri(fileUri.toString());
        changeParamsToSend.getTextDocument().setVersion(++version);
        languageServerWrapper.getInitializedServer()
                .thenAcceptAsync(ls -> ls.getTextDocumentService().didChange(changeParamsToSend));
    }

    @Override
    public void beforeDocumentChange(DocumentEvent event) {
        checkEvent(event);
        if (syncKind == TextDocumentSyncKind.Incremental) {
            // this really needs to happen before event gets actually
            // applied, to properly compute positions
            synchronized (changeEvents) {
                changeEvents.add(createChangeEvent(event));
            }
        }
    }

    private TextDocumentContentChangeEvent createChangeEvent(DocumentEvent event) {
        TextDocumentSyncKind syncKind = getTextDocumentSyncKind();
        switch (syncKind) {
            case None:
                return null;
            case Full: {
                TextDocumentContentChangeEvent changeEvent = new TextDocumentContentChangeEvent();
                changeEvent.setText(event.getDocument().getText());
                return changeEvent;
            }
            case Incremental: {
                TextDocumentContentChangeEvent changeEvent = new TextDocumentContentChangeEvent();
                CharSequence newText = event.getNewFragment();
                int offset = event.getOffset();
                int length = event.getOldLength();
                try {
                    // try to convert the Eclipse start/end offset to LS range.
                    Range range = new Range(LSPIJUtils.toPosition(offset, document),
                            LSPIJUtils.toPosition(offset + length, document));
                    changeEvent.setRange(range);
                    changeEvent.setText(newText.toString());
                    changeEvent.setRangeLength(length);
                } catch (Exception e) {
                    // error while conversion (should never occur)
                    // set the full document text as changes.
                    changeEvent.setText(document.getText());
                }
                return changeEvent;
            }
        }
        return null;
    }

    public void documentSaved(long timestamp) {
        this.modificationStamp = timestamp;
        ServerCapabilities serverCapabilities = languageServerWrapper.getServerCapabilities();
        if (serverCapabilities != null) {
            Either<TextDocumentSyncKind, TextDocumentSyncOptions> textDocumentSync = serverCapabilities.getTextDocumentSync();
            if (textDocumentSync.isRight() && textDocumentSync.getRight().getSave() == null) {
                return;
            }
        }
        TextDocumentIdentifier identifier = new TextDocumentIdentifier(fileUri.toString());
        DidSaveTextDocumentParams params = new DidSaveTextDocumentParams(identifier, document.getText());
        languageServerWrapper.getInitializedServer().thenAcceptAsync(ls -> ls.getTextDocumentService().didSave(params));
    }

    public void documentClosed() {
        // When LS is shut down all documents are being disconnected. No need to send "didClose" message to the LS that is being shut down or not yet started
        if (languageServerWrapper.isActive()) {
            TextDocumentIdentifier identifier = new TextDocumentIdentifier(fileUri.toString());
            DidCloseTextDocumentParams params = new DidCloseTextDocumentParams(identifier);
            languageServerWrapper.getInitializedServer().thenAcceptAsync(ls -> ls.getTextDocumentService().didClose(params));
        }
    }

    /**
     * Returns the text document sync kind capabilities of the server and {@link TextDocumentSyncKind#Full} otherwise.
     *
     * @return the text document sync kind capabilities of the server and {@link TextDocumentSyncKind#Full} otherwise.
     */
    private TextDocumentSyncKind getTextDocumentSyncKind() {
        return syncKind;
    }

    protected long getModificationStamp() {
        return modificationStamp;
    }

    public Document getDocument() {
        return this.document;
    }

    int getVersion() {
        return version;
    }

    private void logDocument(String header, Document document) {
        LOGGER.warn(header + " text='" + document.getText());
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file != null) {
            LOGGER.warn(header + " file=" + file);
        }
    }

    private void checkEvent(DocumentEvent event) {
        if (this.document != event.getDocument()) {
            logDocument("Listener document", this.document);
            logDocument("Event document", event.getDocument());
            throw new IllegalStateException("Synchronizer should apply to only a single document, which is the one it was instantiated for"); //$NON-NLS-1$
        }
    }

}
