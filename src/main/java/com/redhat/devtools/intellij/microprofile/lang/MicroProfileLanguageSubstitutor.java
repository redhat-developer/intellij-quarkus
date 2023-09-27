package com.redhat.devtools.intellij.microprofile.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutor;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MicroProfileLanguageSubstitutor extends LanguageSubstitutor {

    @Override
    public @Nullable Language getLanguage(@NotNull VirtualFile file, @NotNull Project project) {
        if (QuarkusModuleUtil.isQuarkusPropertiesFile(file, project) || QuarkusModuleUtil.isQuarkusYAMLFile(file, project)) {
            return MicroProfileLanguage.INSTANCE;
        }
        return null;
    }
}
