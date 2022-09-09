/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.core.command;

import com.google.gson.JsonObject;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.codelens.LSPCodelensInlayProvider;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.qute.commons.QuteJavaDefinitionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Location;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class QuteJavaDefinitionAction extends QuteAction {
    private static final String PROJECT_URI_ATTR = "projectUri";
    private static final String SOURCE_TYPE_ATTR = "sourceType";
    private static final String SOURCE_FIELD_ATTR = "sourceField";
    private static final String SOURCE_METHOD_ATTR = "sourceMethod";
    private static final String SOURCE_PARAMETER_ATTR = "sourceParameter";
    private static final String DATA_METHOD_INVOCATION_ATTR = "dataMethodInvocation";
    private static System.Logger LOGGER = System.getLogger(QuteJavaDefinitionAction.class.getName());

    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            Command command = e.getData(LSPCodelensInlayProvider.LSP_COMMAND);
            QuteJavaDefinitionParams params = getQuteJavaDefinitionParams(command.getArguments());
            if (params != null) {
                Location location = QuteSupportForTemplate.getInstance().getJavaDefinition(params, PsiUtilsLSImpl.getInstance(e.getProject()), new EmptyProgressIndicator());
                VirtualFile f = VfsUtil.findFileByURL(new URL(location.getUri()));
                if (f != null) {
                    Document document = FileDocumentManager.getInstance().getDocument(f);
                    if (document != null) {
                        OpenFileDescriptor desc = new OpenFileDescriptor(e.getProject(), f, LSPIJUtils.toOffset(location.getRange().getStart(), document));
                        FileEditorManager.getInstance(e.getProject()).openTextEditor(desc, true);
                    }
                }

            }
        } catch (MalformedURLException ex) {
            LOGGER.log(System.Logger.Level.WARNING, ex.getLocalizedMessage(), ex);
        }
    }

    protected String getString(String name, JsonObject obj) {
        return obj.has(name)?obj.get(name).getAsString():null;
    }

    protected boolean getBoolean(String name, JsonObject obj) {
        return obj.has(name)?obj.get(name).getAsBoolean():false;
    }

    private QuteJavaDefinitionParams getQuteJavaDefinitionParams(List<Object> arguments) {
        if (!arguments.isEmpty() && arguments.get(0) instanceof JsonObject) {
            JsonObject obj = ((JsonObject) arguments.get(0));
            String templateFileUri = getString(PROJECT_URI_ATTR, obj);
            String sourceType = getString(SOURCE_TYPE_ATTR, obj);
            QuteJavaDefinitionParams params = new QuteJavaDefinitionParams(sourceType, templateFileUri);
            String sourceField = getString(SOURCE_FIELD_ATTR, obj);
            params.setSourceField(sourceField);
            String sourceMethod = getString(SOURCE_METHOD_ATTR, obj);
            params.setSourceMethod(sourceMethod);
            String methodParameter = getString(SOURCE_PARAMETER_ATTR, obj);
            params.setSourceParameter(methodParameter);
            boolean dataMethodInvocation = getBoolean(DATA_METHOD_INVOCATION_ATTR, obj);
            params.setDataMethodInvocation(dataMethodInvocation);
            return params;
        }
        return null;
    }
}
