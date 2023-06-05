package model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import io.quarkiverse.renarde.oidc.RenardeUser;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "user_table", uniqueConstraints = @UniqueConstraint(columnNames = {"tenantId", "authId"}))
public class User extends PanacheEntity implements RenardeUser {
	
	@Column(nullable = false)
	public String email;
	@Column(unique = true)
	public String userName;
    public String password;
    // non-owning side, so we can add more credentials later
    @OneToOne(mappedBy = "user")
    public WebAuthnCredential webAuthnCredential;
	public String firstName;
	public String lastName;
	public boolean isAdmin;

	@Column(unique = true)
	public String confirmationCode;

	public String tenantId;
	public String authId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	public UserStatus status;
	
	public boolean isRegistered(){
	    return status == UserStatus.REGISTERED;
	}

    @Override
    public Set<String> getRoles() {
        Set<String> roles = new HashSet<>();
        if(isAdmin) {
            roles.add("admin");
        }
        return roles;
    }

    @Override
    public String getUserId() {
        return userName;
    }

    public boolean isOidc() {
        return tenantId != null;
    }

    private boolean isWebAuthn() {
        return webAuthnCredential != null;
    }

    public String getIconType() {
        if(isOidc())
            return "shield-check";
        if(isWebAuthn())
            return "fingerprint";
        return "shield-lock";
    }
    
	//
	// Helpers

    public static User findUnconfirmedByEmail(String email) {
		return find("LOWER(email) = ?1 AND status = ?2", email.toLowerCase(), UserStatus.CONFIRMATION_REQUIRED).firstResult();
	}

	public static User findRegisteredByUserName(String username) {
        return find("LOWER(userName) = ?1 AND status = ?2", username.toLowerCase(), UserStatus.REGISTERED).firstResult();
    }

    public static User findByUserName(String username) {
        return find("LOWER(userName) = ?1", username.toLowerCase()).firstResult();
    }
    
    public static User findByAuthId(String tenantId, String authId) {
        return find("tenantId = ?1 AND authId = ?2", tenantId, authId).firstResult();
    }

    public static User findForContirmation(String confirmationCode) {
        return find("confirmationCode = ?1 AND status = ?2", confirmationCode, UserStatus.CONFIRMATION_REQUIRED).firstResult();
    }
}