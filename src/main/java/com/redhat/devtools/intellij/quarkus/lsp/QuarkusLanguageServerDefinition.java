/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lsp;

import com.github.gtache.lsp.client.LanguageClientImpl;
import com.github.gtache.lsp.client.languageserver.serverdefinition.ExeLanguageServerDefinition;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.redhat.devtools.intellij.quarkus.PluginHelper;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.jetbrains.annotations.NotNull;
import scala.Tuple2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

public class QuarkusLanguageServerDefinition extends ExeLanguageServerDefinition {
  private static final Gson gson = new Gson();

  private class RedirectedOutputStream extends OutputStream {

    private final OutputStream delegate;

    private RedirectedOutputStream(OutputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public void write(int i) throws IOException {
      delegate.write(i);
    }

    @Override
    public void write(@NotNull byte[] b, int off, int len) throws IOException {
      Optional<JsonElement> element = getJson(b, off, len);
      if (element.isPresent()) {
        byte[] content = gson.toJson(element.get()).getBytes("UTF-8");
        delegate.write(content, 0, content.length);
      } else {
        delegate.write(b, off, len);
      }
    }

    private Optional<JsonElement> getJson(@NotNull byte[] b, int off, int len) {
      try {
        JsonObject jsonObject = gson.fromJson(new String(b, off, len, "UTF-8"), JsonObject.class);
        if (jsonObject.has("method") && jsonObject.get("method").getAsString().equals("textDocument/didChange")) {
          JsonObject params = jsonObject.get("params").getAsJsonObject();
          DidChangeTextDocumentParams textDocumentParams = gson.fromJson(params, DidChangeTextDocumentParams.class);
          boolean updated = textDocumentParams.getContentChanges().stream().anyMatch(event -> patchChange(event));
          if (updated) {
            jsonObject.add("params", gson.toJsonTree(textDocumentParams));
            return Optional.of(jsonObject);
          }
        }
      } catch (JsonSyntaxException | UnsupportedEncodingException e) {
      }
      return Optional.empty();
    }

    private boolean patchChange(TextDocumentContentChangeEvent element) {
      Range range = element.getRange();
      if (range.getStart().equals(range.getEnd())) {
        element.setRangeLength(0);
        return true;
      }
      return false;
    }

    @Override
    public void flush() throws IOException {
      delegate.flush();
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }
  }
  public QuarkusLanguageServerDefinition(String ext, String path, String[] args) {
    super(ext, path, args);
  }

  @Override
  public LanguageClientImpl createLanguageClient() {
    return new QuarkusLanguageClient();
  }

  @Override
  public Tuple2<InputStream, OutputStream> start(String workingDir) {
    Tuple2<InputStream, OutputStream> streams = super.start(workingDir);
    if (PluginHelper.isLSPPluginPre154()) {
      OutputStream output = new RedirectedOutputStream(streams._2());
      return streams.copy(streams._1(), output);
    } else {
      return streams;
    }
  }
}
