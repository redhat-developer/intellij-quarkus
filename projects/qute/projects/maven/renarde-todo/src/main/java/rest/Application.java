package rest;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;

@Blocking
public class Application extends Controller {

    @CheckedTemplate
    static class Templates {
        public static native TemplateInstance index();
        public static native TemplateInstance about();
    }

    @Path("/")
    public TemplateInstance index() {
        return Templates.index();
    }

    @Path("/about")
    public TemplateInstance about() {
        return Templates.about();
    }

    @POST
    public void test() {

    }
}
