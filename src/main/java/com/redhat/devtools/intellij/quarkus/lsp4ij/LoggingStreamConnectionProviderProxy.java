package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.redhat.devtools.intellij.quarkus.lsp4ij.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

//TODO: implement LoggingStreamConnectionProviderProxy fully (preferences)
public class LoggingStreamConnectionProviderProxy implements StreamConnectionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingStreamConnectionProviderProxy.class);

    private final StreamConnectionProvider provider;
    private InputStream inputStream;
    private OutputStream outputStream;
    private InputStream errorStream;
    private final String id;
    private File logFile;
    private boolean logToFile = true;
    private boolean logToConsole = false;


    /**
     * Returns whether currently created connections should be logged to file or the
     * standard error stream.
     *
     * @return If connections should be logged
     */
    public static boolean shouldLog(String serverId) {
        return Boolean.getBoolean("com.redhat.devtools.intellij.quarkus.trace");
    }

    public LoggingStreamConnectionProviderProxy(StreamConnectionProvider provider, String serverId) {
        this.provider = provider;
        this.id = serverId;
        this.logFile = getLogFile();
    }

    @Override
    public void start() throws IOException {
        provider.start();
    }

    @Override
    public InputStream getInputStream() {
        if (inputStream != null) {
            return inputStream;
        }
        if (provider.getInputStream() != null) {
            inputStream = new FilterInputStream(provider.getInputStream()) {
                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    int bytes = super.read(b, off, len);
                    byte[] payload = new byte[bytes];
                    System.arraycopy(b, off, payload, 0, bytes);
                    if (logToConsole || logToFile) {
                        String s = "\n[t=" + System.currentTimeMillis() + "] " + id + " to LSP4E:\n" + new String(payload); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        if (logToConsole) {
                            logToConsole(s);
                        }
                        if (logToFile) {
                            logToFile(s);
                        }
                    }
                    return bytes;
                }
            };
        }
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        if (outputStream != null) {
            return outputStream;
        }
        if (provider.getOutputStream() != null) {
            outputStream = new FilterOutputStream(provider.getOutputStream()) {
                @Override
                public void write(byte[] b) throws IOException {
                    if (logToConsole || logToFile) {
                        String s = "\n[t=" + System.currentTimeMillis() + "] LSP4E to " + id + ":\n" + new String(b);  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        if (logToConsole) {
                            logToConsole(s);
                        }
                        if (logToFile) {
                            logToFile(s);
                        }
                    }
                    super.write(b);
                }
            };
        }
        return outputStream;
    }

    @Nullable
    @Override
    public InputStream getErrorStream() {
        if (errorStream != null) {
            return errorStream;
        }
        if (provider.getErrorStream() != null) {
            errorStream = new FilterInputStream(provider.getErrorStream()) {
                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    int bytes = super.read(b, off, len);
                    byte[] payload = new byte[bytes];
                    System.arraycopy(b, off, payload, 0, bytes);
                    if (logToConsole || logToFile) {
                        String s = "\n[t=" + System.currentTimeMillis() + "] Error from " + id + ":\n" + new String(payload); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        if (logToConsole) {
                            logToConsole(s);
                        }
                        if (logToFile) {
                            logToFile(s);
                        }
                    }
                    return bytes;
                }
            };
        }
        return errorStream;
    }

    @Override
    public void stop() {
        provider.stop();
    }

    @Override
    public InputStream forwardCopyTo(InputStream input, OutputStream output) {
        return provider.forwardCopyTo(input, output);
    }

    @Override
    public String getTrace(URI rootUri) {
        return provider.getTrace(rootUri);
    }

    @Override
    public Object getInitializationOptions(URI rootUri) {
        return provider.getInitializationOptions(rootUri);
    }

    @Override
    public Object getExperimentalFeaturesPOJO() {
        return provider.getExperimentalFeaturesPOJO();
    }

    @Override
    public void handleMessage(Message message, LanguageServer languageServer, URI rootURI) {
        provider.handleMessage(message, languageServer, rootURI);
    }

    private void logToConsole(String string) {
        System.out.println(string);
    }

    private void logToFile(String string) {
        if (logFile == null) {
            return;
        }
        if (!logFile.exists()) {
            try {
                if (!logFile.createNewFile()) {
                    throw new IOException(String.format("Failed to create file %s", logFile.toString())); //$NON-NLS-1$
                }
            } catch (IOException e) {
                LOGGER.warn(e.getLocalizedMessage(), e);
            }
        }
        try {
            Files.write(logFile.toPath(), string.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    private File getLogFile() {
        if (logFile != null) {
            return logFile;
        }
        File file = new File(id + ".log"); //$NON-NLS-1$
        if (file.exists() && !(file.isFile() && file.canWrite())) {
            return null;
        }
        return file.getAbsoluteFile();
    }

}
