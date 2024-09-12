/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.template.datamodel;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiClass;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class for data model provider based on class type search which
 * implements some interfaces.
 *
 * @author Angelo ZERR
 */
public abstract class AbstractInterfaceImplementationDataModelProvider extends AbstractDataModelProvider {

    private static final Logger LOGGER = Logger
            .getLogger(AbstractInterfaceImplementationDataModelProvider.class.getName());

    @Override
    protected String[] getPatterns() {
        return getInterfaceNames();
    }

    /**
     * Returns the interface names to search.
     *
     * @return the interface names to search.
     */
    protected abstract String[] getInterfaceNames();

    @Override
    protected Query<? extends Object> createSearchPattern(SearchContext context, String interfaceName) {
        return createInterfaceImplementationSearchPattern(context, interfaceName);
    }

    @Override
    public void collectDataModel(Object match, SearchContext context, ProgressIndicator monitor) {
        Object element = match;
        if (element instanceof PsiClass type) {
            try {
                if (isApplicable(type)) {
                    processType(type, context, monitor);
                }
            } catch (ProcessCanceledException e) {
                //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
                //TODO delete block when minimum required version is 2024.2
                throw e;
            } catch (IndexNotReadyException | CancellationException e) {
                throw e;
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE,
                            "Cannot collect Qute data model for the type '" + type.getQualifiedName() + "'.", e);
                }
            }
        }
    }

    private boolean isApplicable(PsiClass type) {
        PsiClass @NotNull [] superInterfaceNames = type.getInterfaces();
        if (superInterfaceNames == null || superInterfaceNames.length == 0) {
            return false;
        }
        for (String interfaceName : getInterfaceNames()) {
            for (PsiClass superInterfaceName : superInterfaceNames) {
                if (interfaceName.equals(superInterfaceName.getQualifiedName())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected abstract void processType(PsiClass recordElement, SearchContext context, ProgressIndicator monitor);

}
