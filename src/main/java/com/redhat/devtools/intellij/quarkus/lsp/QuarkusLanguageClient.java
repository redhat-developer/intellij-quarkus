package com.redhat.devtools.intellij.quarkus.lsp;

import com.github.gtache.lsp.client.LanguageClientImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.redhat.devtools.intellij.quarkus.search.ModuleAnalyzer;
import com.redhat.quarkus.commons.QuarkusProjectInfo;
import com.redhat.quarkus.commons.QuarkusProjectInfoParams;
import com.redhat.quarkus.commons.QuarkusPropertiesScope;
import com.redhat.quarkus.ls.api.QuarkusLanguageClientAPI;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class QuarkusLanguageClient extends LanguageClientImpl implements QuarkusLanguageClientAPI {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusLanguageClient.class);

  @Override
  public CompletableFuture<QuarkusProjectInfo> getQuarkusProjectInfo(QuarkusProjectInfoParams request) {
    LOGGER.info("Project info for:" + request.getUri() + " scope=" + request.getScope());
    QuarkusProjectInfo result = new QuarkusProjectInfo();
    if (request.getScope() == QuarkusPropertiesScope.classpath) {
      ApplicationManager.getApplication().runReadAction(() -> result.setProperties(ModuleAnalyzer.INSTANCE.getConfigItem(request)));
    } else {
      result.setProperties(new ArrayList<>());
    }
    return CompletableFuture.completedFuture(result);
  }
}
