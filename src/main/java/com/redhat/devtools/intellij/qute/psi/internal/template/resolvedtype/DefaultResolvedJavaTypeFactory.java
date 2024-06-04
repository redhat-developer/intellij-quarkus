/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.template.resolvedtype;

import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.*;
import com.redhat.qute.commons.InvalidMethodReason;
import com.redhat.qute.commons.ResolvedJavaTypeInfo;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverKind;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.JAVA_LANG_OBJECT_TYPE;

/**
 * The default {@link ResolvedJavaTypeInfo} factory.
 *
 * @author Angelo ZERR
 */
public class DefaultResolvedJavaTypeFactory extends AbstractResolvedJavaTypeFactory {

    private static final Logger LOGGER = Logger.getLogger(DefaultResolvedJavaTypeFactory.class.getName());

    private static final IResolvedJavaTypeFactory INSTANCE = new DefaultResolvedJavaTypeFactory();

    public static IResolvedJavaTypeFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isAdaptedFor(ValueResolverKind kind) {
        return true;
    }

    @Override
    protected boolean isValidField(PsiField field, PsiClass type) {
        if (type.isEnum()) {
            return true;
        }
        return field.getModifierList().hasExplicitModifier(PsiModifier.PUBLIC);
    }

    @Override
    protected boolean isValidRecordField(PsiRecordComponent field, PsiClass type) {
        return true;
    }

    /**
     * Returns the reason
     *
     * @param method
     * @param typeName
     * @return
     * @see <a href="https://github.com/quarkusio/quarkus/blob/ce19ff75e9f732ff731bb30c2141b44b42c66050/independent-projects/qute/core/src/main/java/io/quarkus/qute/ReflectionValueResolver.java#L176">https://github.com/quarkusio/quarkus/blob/ce19ff75e9f732ff731bb30c2141b44b42c66050/independent-projects/qute/core/src/main/java/io/quarkus/qute/ReflectionValueResolver.java#L176</a>
     */
    @Override
    protected InvalidMethodReason getValidMethodForQute(PsiMethod method, String typeName) {
        if (JAVA_LANG_OBJECT_TYPE.equals(typeName)) {
            return InvalidMethodReason.FromObject;
        }
        try {
            if ("void".equals(method.getReturnType().getCanonicalText(true))) {
                return InvalidMethodReason.VoidReturn;
            }
            if (method.getModifierList().hasExplicitModifier(PsiModifier.STATIC)) {
                return InvalidMethodReason.Static;
            }
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Error while checking if '" + method.getName() + "' is valid.", e);
        }
        return null;
    }
}