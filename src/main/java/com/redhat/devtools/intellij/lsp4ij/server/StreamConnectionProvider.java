package com.redhat.devtools.intellij.lsp4ij.server;

import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.services.LanguageServer;

import javax.annotation.Nullable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

public interface StreamConnectionProvider {
    public void start() throws IOException;

    public InputStream getInputStream();

    public OutputStream getOutputStream();

    /**
     * Returns the {@link InputStream} connected to the error output of the process
     * running the language server. If the error output is redirected to standard
     * output it returns <code>null</code>.
     *
     * @return the {@link InputStream} connected to the error output of the language
     *         server process or <code>null</code> if it's redirected or process not
     *         yet started.
     */
    public @Nullable InputStream getErrorStream();

    /**
     * Forwards a copy of an {@link InputStream} to an {@link OutputStream}.
     *
     * @param input
     *            the {@link InputStream} that will be copied
     * @param output
     *            the {@link OutputStream} to forward the copy to
     * @return a newly created {@link InputStream} that copies all data to the
     *         provided {@link OutputStream}
     */
    public default InputStream forwardCopyTo(InputStream input, OutputStream output) {
        if (input == null)
            return null;
        if (output == null)
            return input;

        FilterInputStream filterInput = new FilterInputStream(input) {
            @Override
            public int read() throws IOException {
                int res = super.read();
                System.err.print((char) res);
                return res;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int bytes = super.read(b, off, len);
                byte[] payload = new byte[bytes];
                System.arraycopy(b, off, payload, 0, bytes);
                output.write(payload, 0, payload.length);
                return bytes;
            }

            @Override
            public int read(byte[] b) throws IOException {
                int bytes = super.read(b);
                byte[] payload = new byte[bytes];
                System.arraycopy(b, 0, payload, 0, bytes);
                output.write(payload, 0, payload.length);
                return bytes;
            }
        };

        return filterInput;
    }

    /**
     * User provided initialization options.
     */
    public default Object getInitializationOptions(URI rootUri){
        return null;
    }

    /**
     * Returns an object that describes the experimental features supported
     * by the client.
     * @implNote The returned object gets serialized by LSP4J, which itself uses
     *           GSon, so a GSon object can work too.
     * @since 0.12
     * @return an object whose fields represent the different experimental features
     *         supported by the client.
     */
    public default Object getExperimentalFeaturesPOJO() {
        return null;
    }

    /**
     * Provides trace level to be set on language server initialization.<br>
     * Legal values: "off" | "messages" | "verbose".
     *
     * @param rootUri
     *            the workspace root URI.
     *
     * @return the initial trace level to set
     * @see "https://microsoft.github.io/language-server-protocol/specification#initialize"
     */
    public default String getTrace(URI rootUri) {
        return "off"; //$NON-NLS-1$
    }

    public void stop();

    /**
     * Allows to hook custom behavior on messages.
     * @param message a message
     * @param languageServer the language server receiving/sending the message.
     * @param rootUri
     */
    public default void handleMessage(Message message, LanguageServer languageServer, URI rootURI) {}


}
