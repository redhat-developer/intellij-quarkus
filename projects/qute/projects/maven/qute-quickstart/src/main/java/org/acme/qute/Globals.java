package org.acme.qute;

import io.quarkus.qute.TemplateGlobal;

enum Color { RED, GREEN, BLUE }

@TemplateGlobal
public class Globals {

    static int age = 40;

    static String name;

    static Color[] myColors() {
      return new Color[] { Color.RED, Color.BLUE };
    }

    @TemplateGlobal(name = "currentUser")
    static String user() {
       return "Mia";
    }
}