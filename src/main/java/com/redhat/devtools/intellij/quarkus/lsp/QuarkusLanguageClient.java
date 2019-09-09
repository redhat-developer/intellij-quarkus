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
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@JsonSegment("quarkus")
public class QuarkusLanguageClient extends LanguageClientImpl {
  @JsonRequest("projectInfo")
  public CompletableFuture<Object> projectInfo(QuarkusProjectInfoParams request) {
    System.out.println("Received object is:" + request);
    QuarkusProjectInfo result = new QuarkusProjectInfo();
    ApplicationManager.getApplication().runReadAction(() -> result.setProperties(ModuleAnalyzer.INSTANCE.getConfigItem(request)));
    result.setQuarkusProject(!result.getProperties().isEmpty());
    return CompletableFuture.completedFuture(result);
  }
}
