package util;

import java.util.Date;

import javax.enterprise.event.Observes;
import javax.transaction.Transactional;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import model.Todo;
import model.User;
import model.UserStatus;

public class Startup {
    @Transactional
    public void onStartup(@Observes StartupEvent start) {
        System.err.println("Adding user fromage");
        User stef = new User();
        stef.email = "fromage@example.com";
        stef.firstName = "Stef";
        stef.lastName = "Epardaud";
        stef.userName = "fromage";
        stef.password = BcryptUtil.bcryptHash("1q2w3e4r");
        stef.status = UserStatus.REGISTERED;
        stef.isAdmin = true;
        stef.persist();

        Todo todo = new Todo();
        todo.owner = stef;
        todo.task = "Buy cheese";
        todo.done = true;
        todo.doneDate = new Date();
        todo.persist();

        todo = new Todo();
        todo.owner = stef;
        todo.task = "Eat cheese";
        todo.persist();
    }
}
