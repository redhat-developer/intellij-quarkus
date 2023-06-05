package util;

import java.net.URI;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.Response;

import io.quarkiverse.renarde.oidc.RenardeOidcHandler;
import io.quarkiverse.renarde.oidc.RenardeSecurity;
import io.quarkiverse.renarde.oidc.RenardeUser;
import io.quarkiverse.renarde.oidc.RenardeUserProvider;
import io.quarkiverse.renarde.router.Router;
import io.quarkiverse.renarde.util.Flash;
import io.quarkiverse.renarde.util.RedirectException;
import model.User;
import model.UserStatus;
import rest.Application;
import rest.Login;

@ApplicationScoped
public class MyOidcSetup implements RenardeUserProvider, RenardeOidcHandler {

    @Inject
    RenardeSecurity security;
    
    @Inject
    Flash flash;

    @Override
    public RenardeUser findUser(String tenantId, String id) {
        if(tenantId == null || tenantId.equals("manual")) {
            return User.findByUserName(id);
        } else {
            return User.findByAuthId(tenantId, id);
        }
    }

    @Transactional
    @Override
    public void oidcSuccess(String tenantId, String authId) {
        User user = User.findByAuthId(tenantId, authId);
        URI uri;
        if(user == null) {
            // registration
            user = new User();
            user.tenantId = tenantId;
            user.authId = authId;
            
            user.email = security.getOidcEmail();
            // workaround for Twitter
            if(user.email == null)
                user.email = "twitter@example.com";
            user.firstName = security.getOidcFirstName();
            user.lastName = security.getOidcLastName();
            user.userName = security.getOidcUserName();

            user.status = UserStatus.CONFIRMATION_REQUIRED;
            user.confirmationCode = UUID.randomUUID().toString();
            user.persist();

            // go to registration
            uri = Router.getURI(Login::confirm, user.confirmationCode);
        } else if(!user.isRegistered()) {
            // user exists, but not fully registered yet
            // go to registration
            uri = Router.getURI(Login::confirm, user.confirmationCode);
        } else {
            // regular login
            uri = Router.getURI(Application::index);
        }
        throw new RedirectException(Response.seeOther(uri).build());
    }

    @Override
    public void loginWithOidcSession(String tenantId, String authId) {
        RenardeUser user = findUser(tenantId, authId);
        // old cookie, no such user
        if(user == null) {
            flash.flash("message", "Invalid user: "+authId);
            throw new RedirectException(security.makeLogoutResponse());
        }
        // redirect to registration
        URI uri;
        if(!user.isRegistered()) {
            uri = Router.getURI(Login::confirm, ((User)user).confirmationCode);
        } else {
            flash.flash("message", "Already logged in");
            uri = Router.getURI(Application::index);
        }
        throw new RedirectException(Response.seeOther(uri).build());
    }
}
