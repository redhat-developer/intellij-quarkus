/**
 * Copyright 2012 Daniel Marcotte
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.devtools.intellij.qute.lang.format;

import com.intellij.lang.Language;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.FileIndentOptionsProvider;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.qute.lang.QuteFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is a copy/paste from https://github.com/JetBrains/intellij-plugins/blob/master/handlebars/src/com/dmarcotte/handlebars/format/HbFileIndentOptionsProvider.java adapted for Qute.
 */
public class QuteFileIndentOptionsProvider extends FileIndentOptionsProvider {

  @Nullable
  @Override
  public CommonCodeStyleSettings.@Nullable IndentOptions getIndentOptions(@NotNull CodeStyleSettings settings, @NotNull PsiFile file) {
    if (file.getFileType().equals(QuteFileType.QUTE)) {
      VirtualFile virtualFile = file.getVirtualFile();
      Module module = LSPIJUtils.getModule(virtualFile);
      if (module == null) {
        return null;
      }
      Project project = module.getProject();
      FileViewProvider provider = PsiManagerEx.getInstanceEx(project).findViewProvider(virtualFile);
      if (provider instanceof TemplateLanguageFileViewProvider) {
        Language language = ((TemplateLanguageFileViewProvider)provider).getTemplateDataLanguage();
        return settings.getCommonSettings(language).getIndentOptions();
      }
    }
    return null;
  }
}