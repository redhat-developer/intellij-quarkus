package fr.epardaud;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.renarde.oidc.test.MockAppleOidc;
import io.quarkiverse.renarde.oidc.test.MockFacebookOidc;
import io.quarkiverse.renarde.oidc.test.MockGithubOidc;
import io.quarkiverse.renarde.oidc.test.MockGoogleOidc;
import io.quarkiverse.renarde.oidc.test.MockMicrosoftOidc;
import io.quarkiverse.renarde.oidc.test.MockTwitterOidc;
import io.quarkiverse.renarde.oidc.test.RenardeCookieFilter;
import io.quarkiverse.renarde.util.Flash;
import io.quarkiverse.renarde.util.JavaExtensions;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.webauthn.WebAuthnEndpointHelper;
import io.quarkus.test.security.webauthn.WebAuthnHardware;
import io.restassured.filter.Filter;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.json.JsonObject;

@MockFacebookOidc
@MockGoogleOidc
@MockAppleOidc
@MockMicrosoftOidc
@MockTwitterOidc
@MockGithubOidc
@QuarkusTest
public class TodoResourceTest {

    @TestHTTPResource 
    String url;

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void init() {
        mailbox.clear();
    }

    @Test
    public void testMainPage() {
        given()
        .when().get("/")
        .then()
        .statusCode(200)
        .body("html.head.title", is("Welcome to Todos"));
    }

    @Test
    public void testProtectedPage() {
        // cannot go to Todo page
        given()
        .when()
        .redirects().follow(false)
        .get("/Todos/index")
        .then()
        .statusCode(302);
    }

    @Test
    public void testProtectedPageWithInvalidJwt() throws NoSuchAlgorithmException {
        // canary: valid
        String token = Jwt.issuer("https://example.com/issuer")
                .upn("fromage")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        // valid
        given()
        .when()
        .cookie("QuarkusUser", token)
        .log().ifValidationFails()
        .redirects().follow(false)
        .get("/")
        .then()
        .log().ifValidationFails()
        .statusCode(200);
        // expired
        token = Jwt.issuer("https://example.com/issuer")
                .upn("fromage")
                .issuedAt(Instant.now().minus(20, ChronoUnit.DAYS))
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        assertRedirectWithMessage(token, "Login expired, you've been logged out");
        // invalid issuer
        token = Jwt.issuer("https://example.com/other-issuer")
                .upn("fromage")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        assertRedirectWithMessage(token, "Invalid session (bad JWT), you've been logged out");
        // invalid signature
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        token = Jwt.issuer("https://example.com/issuer")
                .upn("fromage")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(10))
                .innerSign(kp.getPrivate()).encrypt(kp.getPublic());
        assertRedirectWithMessage(token, "Invalid session (bad signature), you've been logged out");
        // invalid user
        token = Jwt.issuer("https://example.com/issuer")
                .upn("cheesy")
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofDays(10))
                .innerSign().encrypt();
        assertRedirectWithMessage(token, "Invalid user: cheesy");
    }

    private void assertRedirectWithMessage(String token, String message) {
        // redirect with message
        String flash = given()
        .when()
        .cookie("QuarkusUser", token)
        .log().ifValidationFails()
        .redirects().follow(false)
        .get("/")
        .then()
        .log().ifValidationFails()
        .statusCode(303)
        // logout
        .cookie("QuarkusUser", "")
        .extract().cookie(Flash.FLASH_COOKIE_NAME);
        Map<String, Object> data = Flash.decodeCookieValue(flash);
        Assertions.assertTrue(data.containsKey("message"));
        Assertions.assertEquals(message, data.get("message"));
    }

    @Test
    public void testManualRegistration() {
        String confirmationCode = register("manual");
        
        RenardeCookieFilter cookieFilter = new RenardeCookieFilter();
        completeRegistration(confirmationCode, cookieFilter, "manual", "shield-lock", request -> {
            request
            .formParam("userName", "manual")
            .formParam("password", "1q2w3e4r")
            .formParam("password2", "1q2w3e4r")
            .formParam("firstName", "Stef")
            .formParam("lastName", "Epardaud");
        });
    }

    @Test
    public void testWebAuthnRegistration() {
        String confirmationCode = register("webauthn");

        RenardeCookieFilter cookieFilter = new RenardeCookieFilter();
        WebAuthnHardware token = new WebAuthnHardware();
        String challenge = WebAuthnEndpointHelper.invokeRegistration("webauthn", cookieFilter);
        
        JsonObject registrationJson = token.makeRegistrationJson(challenge);
        
        completeRegistration(confirmationCode, cookieFilter, "webauthn", "fingerprint", request -> {
            WebAuthnEndpointHelper.addWebAuthnRegistrationFormParameters(request, registrationJson);
            request
            .formParam("userName", "webauthn")
            .formParam("firstName", "Stef")
            .formParam("lastName", "Epardaud");
        });
        
        // now try logging in
        challenge = WebAuthnEndpointHelper.invokeLogin("webauthn", cookieFilter);
        
        JsonObject loginJson = token.makeLoginJson(challenge);
        testManualLogin(cookieFilter, "webauthn", "fingerprint", request -> {
            WebAuthnEndpointHelper.addWebAuthnLoginFormParameters(request, loginJson);
            request
            .formParam("userName", "webauthn");
        });
    }

    private void completeRegistration(String confirmationCode, Filter cookieFilter, String userName, String icon, Consumer<RequestSpecification> completeCustomiser) {
        // confirm form action
        RequestSpecification completeRequest = given()
        .when()
        .queryParam("confirmationCode", confirmationCode);
        
        completeCustomiser.accept(completeRequest);
        
        completeRequest
        .log().ifValidationFails()
        .filter(cookieFilter)
        .redirects().follow(false)
        .post("/Login/complete")
        .then()
        .log().ifValidationFails()
        .statusCode(303)
        .cookie("QuarkusUser")
        .header("Location", url+"Login/welcome");

        testLoggedIn(userName, icon, cookieFilter);
    }

    private String register(String userName) {
        // register email
        given()
        .when()
        .formParam("email", userName+"@example.com")
        .post("/Login/register")
        .then()
        .statusCode(200)
        .body("html.head.title", is("Check your email for confirmation"))
        .body(containsString("An email has been sent to <b>"+userName+"@example.com</b>"));

        // get the confirmation email
        List<Mail> mails = mailbox.getMessagesSentTo(userName+"@example.com");
        Assertions.assertEquals(1, mails.size());
        Mail mail = mails.get(0);
        Assertions.assertNotNull(mail.getText());
        Assertions.assertNotNull(mail.getHtml());
        String linkStart = "If you want to register, complete your registration by going to the following address:\n"
                + "\n" + url;
        int absoluteUriIndex = mail.getText().indexOf(linkStart) + linkStart.length() - 1;
        Assertions.assertTrue(absoluteUriIndex > -1, "Failed to find confirmation URI in email: "+mail.getText());
        String confirmationPath = mail.getText().substring(absoluteUriIndex);
        Assertions.assertTrue(confirmationPath.startsWith("/Login/confirm?confirmationCode="), "Failed to parse confirmation path: "+confirmationPath);
        Assertions.assertTrue(confirmationPath.indexOf('\n') > -1);
        String confirmationCode = confirmationPath.substring("/Login/confirm?confirmationCode=".length(), confirmationPath.indexOf('\n'));
        Assertions.assertFalse(confirmationCode.isEmpty());

        // confirm page
        given()
        .when()
        .queryParam("confirmationCode", confirmationCode)
        .get("/Login/confirm")
        .then()
        .statusCode(200)
        .body("html.head.title", is("Complete registration"));
        
        return confirmationCode;
    }

    private void testLoggedIn(String userName, String icon, Filter cookieFilter) {
        // welcome page
        given()
        .when()
        .filter(cookieFilter)
        .get("/Login/welcome")
        .then()
        .statusCode(200)
        .body(containsString("<title>Home</title>"))
        // alert
        .body(containsString("Welcome, "+userName))
        // user gravatar in menu
        .body(containsString("<span class=\"user-link\" title=\""+userName+"\">\n"
                + "<img src=\"https://www.gravatar.com/avatar/"+JavaExtensions.gravatarHash(userName+"@example.com")+"?s=20\"/>\n"
                + userName+"<i class=\"bi bi-"+icon+"\"></i></span>"))
        // Todo link
        .body(containsString("<a class=\"nav-link\" aria-current=\"page\" href=\"/Todos/index\">Todos</a>"))
        // Logout link
        .body(containsString("<a class=\"nav-link\" aria-current=\"page\" href=\"/_renarde/security/logout\">Logout</a>"));

        // can go to Todo page
        given()
        .when()
        .filter(cookieFilter)
        .get("/Todos/index")
        .then()
        .statusCode(200);

        // now logout
        given()
        .when()
        .filter(cookieFilter)
        .redirects().follow(false)
        .get("/_renarde/security/logout")
        .then()
        .statusCode(303)
        // go home
        .header("Location", url)
        // clear cookie
        .cookie("QuarkusUser", "");

    }
    
    @Test
    public void testManualLogin() {
        RenardeCookieFilter cookieFilter = new RenardeCookieFilter();
        testManualLogin(cookieFilter, "fromage", "shield-lock", request -> {
            request
            .formParam("userName", "fromage")
            .formParam("password", "1q2w3e4r");
        });
    }

    private void testManualLogin(Filter cookieFilter, String userName, String icon, Consumer<RequestSpecification> requestCustomiser) {
        // login form action
        RequestSpecification request = given()
        .when();
        requestCustomiser.accept(request);
        request
        .filter(cookieFilter)
        .redirects().follow(false)
        .post("/Login/manualLogin")
        .then()
        .statusCode(303)
        .cookie("QuarkusUser")
        .header("Location", url);

        testLoggedIn(userName, icon, cookieFilter);
    }

    private void oidcTest(String provider, String email, String firstName, String lastName, String userName) {
        RenardeCookieFilter cookieFilter = new RenardeCookieFilter();
        ValidatableResponse response = follow("/_renarde/security/login-"+provider, cookieFilter);
        response.statusCode(200)
        .body(containsString("Complete registration for "+email))
        // lastname and username
        .body(containsString("value=\""+lastName+"\"/>"))
        // firstname
        .body(containsString("value=\""+firstName+"\"/>"))
        ;

        Assertions.assertNotNull(findCookie(cookieFilter.getCookieStore(), "q_session_"+provider));
        
        String body = response.extract().body().asString();
        String clue = "<form action=\"/Login/complete?confirmationCode=";
        int clueIndex = body.indexOf(clue);
        Assertions.assertTrue(clueIndex > -1);
        int codeIndex = clueIndex + clue.length();
        String confirmationCode = body.substring(codeIndex, body.indexOf('"', codeIndex));
        
        finishConfirmation(cookieFilter, confirmationCode,
                           firstName, lastName, userName, email, "q_session_"+provider);
        
    }

    @Test
    public void githubLoginTest() {
        oidcTest("github", "octocat@github.com", "monalisa", "octocat", "octocat");
    }

    @Test
    public void twitterLoginTest() {
        oidcTest("twitter", "twitter@example.com", "Foo", "Bar", "TwitterUser");
    }

    @Test
    public void googleLoginTest() {
        oidcTest("google", "google@example.com", "Foo", "Bar", "GoogleUser");
    }

    @Test
    public void microsoftLoginTest() {
        oidcTest("microsoft", "microsoft@example.com", "Foo", "Bar", "MicrosoftUser");
    }

    @Test
    public void facebookLoginTest() {
        oidcTest("facebook", "facebook@example.com", "Foo", "Bar", "FacebookUser");
    }

    @Test
    public void appleLoginTest() {
        RenardeCookieFilter cookieFilter = new RenardeCookieFilter();
        ValidatableResponse response = follow("/_renarde/security/login-apple", cookieFilter);
        JsonPath json = response.statusCode(200)
            .extract().body().jsonPath();
        String code = json.get("code");
        String state = json.get("state");
        
        String location = given()
                .when()
                .filter(cookieFilter)
                .formParam("state", state)
                .formParam("code", code)
                // can't follow redirects due to cookies
                .redirects().follow(false)
                // must be precise and not contain an encoding: probably needs fixing in the OIDC side
                .contentType("application/x-www-form-urlencoded")
                .log().ifValidationFails()
                .post("/_renarde/security/oidc-success")
                .then()
                .log().ifValidationFails()
                .statusCode(302)
                .extract().header("Location");
        // now move on to the GET, but make sure we go over http
        ValidatableResponse completeResponse = follow(location.replace("https://", "http://"), cookieFilter)
            .body(containsString("Complete registration for apple@example.com"))
            // no name, username from apple
            ;

        Assertions.assertNotNull(findCookie(cookieFilter.getCookieStore(), "q_session_apple"));

        String body = completeResponse.extract().body().asString();
        String clue = "<form action=\"/Login/complete?confirmationCode=";
        int clueIndex = body.indexOf(clue);
        Assertions.assertTrue(clueIndex > -1);
        int codeIndex = clueIndex + clue.length();
        String confirmationCode = body.substring(codeIndex, body.indexOf('"', codeIndex));

        finishConfirmation(cookieFilter, confirmationCode,
                           "Foo", "Bar", "AppleUser", "apple@example.com", "q_session_apple");
    }

    private void finishConfirmation(RenardeCookieFilter cookieFilter, String confirmationCode, 
                                    String firstName, String lastName, String userName, String email,
                                    String cookieName) {
        // confirm form action
        given()
        .when()
        .queryParam("confirmationCode", confirmationCode)
        .formParam("userName", userName)
        .formParam("firstName", firstName)
        .formParam("lastName", lastName)
        .filter(cookieFilter)
        .redirects().follow(false)
        .post("/Login/complete")
        .then()
        .statusCode(303)
        .header("Location", url+"Login/welcome");

        // welcome page
        given()
        .when()
        .filter(cookieFilter)
        .get("/Login/welcome")
        .then()
        .statusCode(200)
        .body(containsString("<title>Home</title>"))
        // alert
        .body(containsString("Welcome, "+userName))
        // user gravatar in menu
        .body(containsString("<span class=\"user-link\" title=\""+userName+"\">\n"
                + "<img src=\"https://www.gravatar.com/avatar/"+JavaExtensions.gravatarHash(email)+"?s=20\"/>\n"
                + userName+"<i class=\"bi bi-shield-check\"></i></span>"))
        // Todo link
        .body(containsString("<a class=\"nav-link\" aria-current=\"page\" href=\"/Todos/index\">Todos</a>"))
        // Logout link
        .body(containsString("<a class=\"nav-link\" aria-current=\"page\" href=\"/_renarde/security/logout\">Logout</a>"));

        // can go to Todo page
        given()
        .when()
        .filter(cookieFilter)
        .get("/Todos/index")
        .then()
        .statusCode(200);

        // now logout
        given()
        .when()
        .filter(cookieFilter)
        .redirects().follow(false)
        .get("/_renarde/security/logout")
        .then()
        .statusCode(303)
        // go home
        .header("Location", url)
        // clear cookie
        .cookie(cookieName, "");
    }

    private Object findCookie(CookieStore cookieStore, String name) {
        for (Cookie cookie : cookieStore.getCookies()) {
            if(cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    private ValidatableResponse follow(String uri, RenardeCookieFilter cookieFilter) {
        do {
            // make sure we turn any https into http, because some providers force https
            if(uri.startsWith("https://")) {
                uri = "http" + uri.substring(5);
            }
            ValidatableResponse response = given()
                    .when()
                    .filter(cookieFilter)
                    // mandatory for Location redirects
                    .urlEncodingEnabled(false)
                    .redirects().follow(false)
                    .log().ifValidationFails()
                    .get(uri)
                    .then()
                    .log().ifValidationFails();
            ExtractableResponse<Response> extract = response.extract();
            if(extract.statusCode() == 302
                    || extract.statusCode() == 303) {
                uri = extract.header("Location");
            } else {
                return response;
            }
        } while (true);
    }
}