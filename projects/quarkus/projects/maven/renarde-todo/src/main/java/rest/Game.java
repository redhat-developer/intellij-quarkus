package rest;

import javax.ws.rs.Path;
import io.quarkiverse.renarde.Controller;

@Path("/play")
public class Game extends Controller {

    @Path("/id")
    public String endpoint() {
        return "id";
    }

    public String start() {
        return "start";
    }
}