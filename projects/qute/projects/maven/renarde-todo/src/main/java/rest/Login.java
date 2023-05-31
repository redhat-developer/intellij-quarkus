package rest;

import java.util.UUID;

import javax.inject.Inject;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.BeanParam;
import javax.ws.rs.POST;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.hibernate.validator.constraints.Length;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import email.Emails;
import io.quarkiverse.renarde.oidc.ControllerWithUser;
import io.quarkiverse.renarde.oidc.RenardeSecurity;
import io.quarkiverse.renarde.router.Router;
import io.quarkiverse.renarde.util.StringUtils;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.webauthn.WebAuthnLoginResponse;
import io.quarkus.security.webauthn.WebAuthnRegisterResponse;
import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.smallrye.common.annotation.Blocking;
import io.vertx.ext.auth.webauthn.Authenticator;
import io.vertx.ext.web.RoutingContext;
import model.User;
import model.UserStatus;
import model.WebAuthnCredential;

@Blocking
public class Login extends ControllerWithUser<User> {
    @Inject
    RenardeSecurity security;

    @Inject
    WebAuthnSecurity webAuthnSecurity;

    @CheckedTemplate
    static class Templates {
        public static native TemplateInstance login();
        public static native TemplateInstance register(String email);
        public static native TemplateInstance confirm(User newUser);
        public static native TemplateInstance logoutFirst();
        public static native TemplateInstance welcome();
    }

    /**
     * Login page
     */
    public TemplateInstance login() {
        return Templates.login();
    }

    /**
     * Welcome page at the end of registration
     */
    @Authenticated
    public TemplateInstance welcome() {
        return Templates.welcome();
    }

    /**
     * Manual login form
     */
    @POST
    public Response manualLogin(@NotBlank @RestForm String userName,
            @RestForm String password,
            @BeanParam WebAuthnLoginResponse webAuthnResponse,
            RoutingContext ctx) {
        if(webAuthnResponse.isSet()) {
            validation.required("webAuthnId", webAuthnResponse.webAuthnId);
            validation.required("webAuthnRawId", webAuthnResponse.webAuthnRawId);
            validation.required("webAuthnResponseClientDataJSON", webAuthnResponse.webAuthnResponseClientDataJSON);
            validation.required("webAuthnResponseAuthenticatorData", webAuthnResponse.webAuthnResponseAuthenticatorData);
            validation.required("webAuthnResponseSignature", webAuthnResponse.webAuthnResponseSignature);
            // UserHandle not required
            validation.required("webAuthnType", webAuthnResponse.webAuthnType);
        } else {
            validation.required("password", password);
        }
        if(validationFailed()) {
            login();
        }
        User user = User.findRegisteredByUserName(userName);
        if(user == null) {
            validation.addError("userName", "Invalid username/pasword");
            prepareForErrorRedirect();
            login();
        }
        if(!webAuthnResponse.isSet()) {
            if(!BcryptUtil.matches(password, user.password)) {
                validation.addError("userName", "Invalid username/pasword");
                prepareForErrorRedirect();
                login();
            }
        } else {
            // This is sync anyway
            Authenticator authenticator = this.webAuthnSecurity.login(webAuthnResponse, ctx)
                .await().indefinitely();
            // bump the auth counter
            user.webAuthnCredential.counter = authenticator.getCounter();
        }
        NewCookie cookie = security.makeUserCookie(user);
        return Response.seeOther(Router.getURI(Application::index)).cookie(cookie).build();
    }

    /**
     * Manual registration form, sends confirmation email
     */
    @POST
    public TemplateInstance register(@RestForm @NotBlank @Email String email) {
        if(validationFailed())
            login();
        User newUser = User.findUnconfirmedByEmail(email);
        if(newUser == null) {
            newUser = new User();
            newUser.email = email;
            newUser.status = UserStatus.CONFIRMATION_REQUIRED;
            newUser.confirmationCode = UUID.randomUUID().toString();
            newUser.persist();
        }
        // send the confirmation code again
        Emails.confirm(newUser);
        return Templates.register(email);
    }


    /**
     * Confirmation form, once email is verified, to add user info
     */
    public TemplateInstance confirm(@RestPath String confirmationCode){
        checkLogoutFirst();
        User newUser = checkConfirmationCode(confirmationCode);
        return Templates.confirm(newUser);
    }

    private void checkLogoutFirst() {
        if(getUser() != null) {
            logoutFirst();
        }
    }

    /**
     * Link to logout page
     */
    public TemplateInstance logoutFirst() {
        return Templates.logoutFirst();
    }

    private User checkConfirmationCode(String confirmationCode) {
        // can't use error reporting as those are query parameters and not form fields
        if(StringUtils.isEmpty(confirmationCode)){
            flash("message", "Missing confirmation code");
            flash("messageType", "error");
            redirect(Application.class).index();
        }
        User user = User.findForContirmation(confirmationCode);
        if(user == null){
            flash("message", "Invalid confirmation code");
            flash("messageType", "error");
            redirect(Application.class).index();
        }
        return user;
    }

    @POST
    public Response complete(@RestQuery String confirmationCode,
            @RestForm @NotBlank String userName,
            @RestForm @Length(min = 8) String password,
            @RestForm @Length(min = 8) String password2,
            @BeanParam WebAuthnRegisterResponse webAuthnResponse,
            @RestForm @NotBlank String firstName,
            @RestForm @NotBlank String lastName,
            RoutingContext ctx) {
        checkLogoutFirst();
        User user = checkConfirmationCode(confirmationCode);

        if(validationFailed())
            confirm(confirmationCode);

        // is it OIDC?
        if(!user.isOidc()) {
            if(!webAuthnResponse.isSet()) {
                validation.required("password", password);
                validation.required("password2", password2);
                validation.equals("password", password, password2);
            } else {
                validation.required("webAuthnId", webAuthnResponse.webAuthnId);
                validation.required("webAuthnRawId", webAuthnResponse.webAuthnRawId);
                validation.required("webAuthnResponseAttestationObject", webAuthnResponse.webAuthnResponseAttestationObject);
                validation.required("webAuthnResponseClientDataJSON", webAuthnResponse.webAuthnResponseClientDataJSON);
                validation.required("webAuthnType", webAuthnResponse.webAuthnType);
            }
        }

        if(User.findRegisteredByUserName(userName) != null)
            validation.addError("userName", "User name already taken");
        if(validationFailed())
            confirm(confirmationCode);

        if(!user.isOidc()) {
            if(!webAuthnResponse.isSet()) {
                user.password = BcryptUtil.bcryptHash(password);
            } else {
                // this is sync
                Authenticator authenticator = webAuthnSecurity.register(webAuthnResponse, ctx).await().indefinitely();
                WebAuthnCredential creds = new WebAuthnCredential(authenticator, user);
                creds.persist();
            }
        }
        user.userName = userName;
        user.firstName = firstName;
        user.lastName = lastName;
        user.confirmationCode = null;
        user.status = UserStatus.REGISTERED;

        ResponseBuilder responseBuilder = Response.seeOther(Router.getURI(Login::welcome));
        if(!user.isOidc()) {
            NewCookie cookie = security.makeUserCookie(user);
            responseBuilder.cookie(cookie);
        }
        return responseBuilder.build();
    }
}
