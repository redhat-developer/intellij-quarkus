package com.redhat.devtools.intellij.quarkus.module;

import java.util.ArrayList;
import java.util.List;

public class QuarkusCategory {
    private String name;
    private List<QuarkusExtension> extensions = new ArrayList<>();

    public QuarkusCategory(String name) {
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<QuarkusExtension> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<QuarkusExtension> extensions) {
        this.extensions = extensions;
    }
}
