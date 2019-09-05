package com.redhat.devtools.intellij.quarkus.facet;

import com.intellij.framework.library.LibraryBasedFrameworkType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class QuarkusFrameworkType extends LibraryBasedFrameworkType {
    protected QuarkusFrameworkType() {
        super("quarkus", QuarkusLibraryType.class);
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return "Quarkus";
    }
}
