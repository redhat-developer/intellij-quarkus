package rest;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import io.quarkiverse.renarde.oidc.ControllerWithUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import model.Todo;
import model.User;

@Blocking
@Authenticated
public class Todos extends ControllerWithUser<User> {

    @CheckedTemplate
    static class Templates {
        public static native TemplateInstance index(List<Todo> todos);
    }
    
    public TemplateInstance index() {
        List<Todo> todos = Todo.findByOwner(getUser());
        return Templates.index(todos);
    }
    
    @POST
    public void delete(@RestPath Long id) {
        Todo todo = Todo.findById(id);
        notFoundIfNull(todo);
        if(todo.owner != getUser())
            notFound();
        todo.delete();
        flash("message", "Task deleted");
        index();
    }
    
    @POST
    public void done(@RestPath Long id) {
        Todo todo = Todo.findById(id);
        notFoundIfNull(todo);
        if(todo.owner != getUser())
            notFound();
        todo.done = !todo.done;
        if(todo.done)
            todo.doneDate = new Date();
        flash("message", "Task updated");
        index();
    }

    @POST
    public void add(@NotBlank @RestForm String task) {
        if(validationFailed()) {
            index();
        }
        Todo todo = new Todo();
        todo.task = task;
        todo.owner = getUser();
        todo.persist();
        flash("message", "Task added");
        index();
    }
}