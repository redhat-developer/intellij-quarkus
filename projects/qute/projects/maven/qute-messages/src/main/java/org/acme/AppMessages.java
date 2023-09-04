package org.acme;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle
public interface AppMessages {

    @Message("Hello {name ?: 'Qute'}")
    String hello_name(String name);

    @Message("Goodbye {name}!")
    String goodbye(String name);

    @Message("Hello!")
    String hello();

    @Message
    String hello2();
}
