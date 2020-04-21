package com.redhat.devtools.intellij.quarkus.liveTemplates;

import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class QuarkusApplicationPropertiesContext extends TemplateContextType {

    protected QuarkusApplicationPropertiesContext() {
        super("QUARKUS_APPLICATION_PROPERTIES", "Quarkus properties");
    }

    @Override
    public boolean isInContext(@NotNull PsiFile file, int offset) {
        return file.getName().endsWith("application.properties");
    }

}
