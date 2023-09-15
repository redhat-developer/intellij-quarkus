package org.acme.qute;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import java.util.List;

@CheckedTemplate(ignoreFragments = true)
public class ItemTemplatesIgnoreFragments {

    static native TemplateInstance items2(List<Item> items);
    static native TemplateInstance items2$id1(List<Item> items);
    static native TemplateInstance items2$id2(List<Item> items);
}