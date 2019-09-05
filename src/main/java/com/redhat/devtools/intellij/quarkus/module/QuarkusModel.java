package com.redhat.devtools.intellij.quarkus.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuarkusModel {
    private List<QuarkusCategory> categories = new ArrayList<>();

    public QuarkusModel(List<QuarkusExtension> extensions) {
        Collections.sort(extensions, (e1, e2) -> e1.getOrder() - e1.getOrder());
        final QuarkusCategory[] currentCategory = {null};
        extensions.forEach(e -> {
            if (currentCategory[0] == null || !e.getCategory().equals(currentCategory[0].getName())) {
                currentCategory[0] = new QuarkusCategory(e.getCategory());
                categories.add(currentCategory[0]);
            }
            currentCategory[0].getExtensions().add(e);
        });
    }
    public List<QuarkusCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<QuarkusCategory> categories) {
        this.categories = categories;
    }
}
