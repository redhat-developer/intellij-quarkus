package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.webauthn.AttestationCertificates;
import io.vertx.ext.auth.webauthn.Authenticator;
import model.WebAuthnCertificate;
import model.WebAuthnCredential;

@Blocking
@ApplicationScoped
public class MyWebAuthnSetup implements WebAuthnUserProvider {

    @Transactional
    @Override
    public Uni<List<Authenticator>> findWebAuthnCredentialsByUserName(String userId) {
        return Uni.createFrom().item(toAuthenticators(WebAuthnCredential.findByUserId(userId)));
    }

    @Transactional
    @Override
    public Uni<List<Authenticator>> findWebAuthnCredentialsByCredID(String credId) {
        WebAuthnCredential creds = WebAuthnCredential.findByCredId(credId);
        if(creds == null)
            return Uni.createFrom().item(Collections.emptyList());
        return Uni.createFrom().item(Arrays.asList(toAuthenticator(creds)));
    }

    @Override
    public Uni<Void> updateOrStoreWebAuthnCredentials(Authenticator authenticator) {
        // do nothing here, it's done in login/register
        return Uni.createFrom().nullItem();
    }

    private static List<Authenticator> toAuthenticators(List<WebAuthnCredential> dbs) {
        List<Authenticator> ret = new ArrayList<>(dbs.size());
        for (WebAuthnCredential db : dbs) {
            ret.add(toAuthenticator(db));
        }
        return ret;
    }

    private static Authenticator toAuthenticator(WebAuthnCredential credential) {
        Authenticator ret = new Authenticator();
        ret.setAaguid(credential.aaguid);
        AttestationCertificates attestationCertificates = new AttestationCertificates();
        attestationCertificates.setAlg(credential.alg);
        List<String> x5cs = new ArrayList<>(credential.x5c.size());
        for (WebAuthnCertificate webAuthnCertificate : credential.x5c) {
            x5cs.add(webAuthnCertificate.x5c);
        }
        ret.setAttestationCertificates(attestationCertificates);
        ret.setCounter(credential.counter);
        ret.setCredID(credential.credID);
        ret.setFmt(credential.fmt);
        ret.setPublicKey(credential.publicKey);
        ret.setType(credential.type);
        ret.setUserName(credential.userName);
        return ret;
    }
}
