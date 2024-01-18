/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.projectWizard;

import com.intellij.ide.starters.shared.TextValidationFunction;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Objects;

/**
 * Validate a {@link JTextField} against several {@link TextValidationFunction}s
 */
public class TextFieldValidator {

    private final JTextField textField;
    private final TextValidationFunction[] validations;

    public TextFieldValidator(@NotNull JTextField textField, @NotNull TextValidationFunction ... validations){
        this.textField = textField;
        this.validations = validations;
    }

    /**
     * Validates the {@link JTextField} bound to this validator.
     * @return a {@link ValidationInfo} instance for the first error message, if validation failed, <code>null</code> otherwise.
     */
    public @Nullable ValidationInfo validate() {
        return Arrays.stream(validations).map(v -> v.checkText(textField.getText()))
                .filter(Objects::nonNull).findFirst().map(t -> new ValidationInfo(t, textField)).orElse(null);
    }
}
