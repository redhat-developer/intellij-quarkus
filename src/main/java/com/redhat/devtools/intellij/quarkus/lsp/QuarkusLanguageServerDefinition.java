package com.redhat.devtools.intellij.quarkus.lsp;

import com.github.gtache.lsp.client.LanguageClientImpl;
import com.github.gtache.lsp.client.connection.StreamConnectionProvider;
import com.github.gtache.lsp.client.languageserver.serverdefinition.ExeLanguageServerDefinition;
import scala.collection.JavaConverters;

import java.util.Arrays;

public class QuarkusLanguageServerDefinition extends ExeLanguageServerDefinition {
  public QuarkusLanguageServerDefinition(String ext, String path, String[] args) {
    super(ext, path, args);
  }

  @Override
  public LanguageClientImpl createLanguageClient() {
    return new QuarkusLanguageClient();
  }
}
