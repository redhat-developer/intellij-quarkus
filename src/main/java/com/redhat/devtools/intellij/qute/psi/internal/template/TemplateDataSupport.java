package com.redhat.devtools.intellij.qute.psi.internal.template;

import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.qute.commons.datamodel.DataModelBaseTemplate;
import org.eclipse.lsp4j.Location;

import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelTemplate;

/**
 * Support with template data method invocation template#data(name, value).
 *
 * @author Angelo ZERR
 */
public class TemplateDataSupport {

    private static final Logger LOGGER = Logger.getLogger(TemplateDataSupport.class.getName());

    /**
     * Search all method invocation of template#data(name, value) to collect data
     * model parameters for the given template.
     *
     * @param fieldOrMethod the template field (ex : Template hello;) or method
     *                      which returns TemplateInstance.
     * @param template      the data model template to update with collect of data
     *                      model parameters.
     * @param monitor       the progress monitor.
     */
    public static void collectParametersFromDataMethodInvocation(PsiMember fieldOrMethod,
                                                                 DataModelBaseTemplate<DataModelParameter> template, ProgressIndicator monitor) {
        try {
            search(fieldOrMethod, new TemplateDataCollector(template, monitor), monitor);
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Error while getting collecting template parameters for '" + fieldOrMethod.getName() + "'.",
                    e);
        }
    }

    private static void search(PsiMember fieldOrMethod, TemplateDataVisitor visitor, ProgressIndicator monitor) {
        boolean searchInJavaProject = isSearchInJavaProject(fieldOrMethod);
        SearchScope searchScope = GlobalSearchScope.projectScope(fieldOrMethod.getProject());
        if (!searchInJavaProject) {
            searchScope = searchScope.intersectWith(new LocalSearchScope(fieldOrMethod.getContainingClass()));
        }
        Query<PsiReference> query = ReferencesSearch.search(fieldOrMethod, searchScope);
        query.forEach((Consumer<? super PsiReference>) psiReference -> {
            PsiMethodCallExpression methodCall = PsiTreeUtil.getParentOfType(psiReference.getElement(), PsiMethodCallExpression.class);
            if (methodCall != null) {
                PsiMethod method = PsiTreeUtil.getParentOfType(methodCall, PsiMethod.class);
                if (method != null) {
                    visitor.setMethod(method);
                    methodCall.accept(visitor);
                }
            }
        });
    }

    /**
     * Returns true if the search of method invocation of template#data(name, value)
     * must be done in Java project or inside the compilation unit of the
     * field/method.
     *
     * @param fieldOrMethod
     * @return
     */
    private static boolean isSearchInJavaProject(PsiMember fieldOrMethod) {
        if (fieldOrMethod instanceof PsiField) {
            return false;
        }
        PsiClass type = fieldOrMethod.getContainingClass();
        boolean innerClass = type.getContainingClass() != null;
        return innerClass;
    }

    public static Location getDataMethodInvocationLocation(PsiMember fieldOrMethod, String parameterName, IPsiUtils utils,
                                                           ProgressIndicator monitor) {
        try {
            TemplateDataLocation dataLocation = new TemplateDataLocation(parameterName, utils);
            search(fieldOrMethod, dataLocation, monitor);
            return dataLocation.getLocation();
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Error while getting location template.data for '" + fieldOrMethod.getName() + "'.", e);
            return null;
        }
    }
}
