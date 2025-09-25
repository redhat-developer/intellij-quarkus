package com.redhat.devtools.intellij.quarkus.psi.internal.builditems;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import com.redhat.microprofile.psi.internal.quarkus.renarde.java.RenardeConstants;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class QuarkusBuildItemUtils {

    private QuarkusBuildItemUtils() {

    }

    /**
     * Returns a set of all classes in the given project that extend Renarde's
     * <code>Controller</code> class.
     *
     * @param project the project to search in
     * @param monitor the progress monitor
     * @return a set of all classes in the given project that extend Renarde's
     * <code>Controller</code> class
     */
    public static Set<PsiClass> getAllBuildItemClasses(Module project, ProgressIndicator monitor) {
        PsiClass buildItemType = PsiTypeUtils.findType(project, QuarkusConstants.QUARKUS_BUILD_ITEM_CLASS_NAME);
        if (buildItemType == null) {
            return Collections.emptySet();
        }

        Set<PsiClass> types = new HashSet<>();
        DefinitionsScopedSearch.search(buildItemType, ProjectScope.getAllScope(project.getProject()), true)
                .forEach(element -> {
                    if (element instanceof PsiClass type && isValidBuildItem(type)) {
                        types.add(type);
                    }
                });
        return types;
    }


    public static boolean isValidBuildItem(PsiClass psiClass) {
        return psiClass.hasModifierProperty(PsiModifier.FINAL)
                || psiClass.hasModifierProperty(PsiModifier.ABSTRACT);
    }
}
