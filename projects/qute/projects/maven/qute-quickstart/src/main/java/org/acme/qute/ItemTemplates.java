package org.acme.qute;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import java.util.List;

@CheckedTemplate
public class ItemTemplates {

    static native TemplateInstance items(List<Item> items);
    static native TemplateInstance items$id1(List<Item> items);
    static native TemplateInstance items3$id2(List<Item> items);
    static native TemplateInstance items3$(List<Item> items);
}