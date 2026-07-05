package com.docsynth.domain.user;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "oidc_subject", nullable = false)
    private String oidcSubject;

    @Column(name = "oidc_issuer", nullable = false)
    private String oidcIssuer;

    @Column(nullable = false)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    protected User() {}

    public User(String oidcSubject, String oidcIssuer, String email, String displayName) {
        this.oidcSubject = oidcSubject;
        this.oidcIssuer = oidcIssuer;
        this.email = email;
        this.displayName = displayName;
    }

    public UUID getId() { return id; }
    public UserId getUserId() { return new UserId(id); }
    public String getOidcSubject() { return oidcSubject; }
    public String getOidcIssuer() { return oidcIssuer; }
    public String getEmail() { return email; }
    public Instant getLastSeenAt() { return lastSeenAt; }

    public void markSeen() {
        this.lastSeenAt = Instant.now();
    }
}
