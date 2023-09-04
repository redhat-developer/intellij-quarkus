package org.acme;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle("msg2")
public interface App2Messages {

    @Message("HELLO!")
    String hello();

    @Message
    String hello2();
}
