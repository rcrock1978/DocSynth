package com.docsynth.infrastructure.persistence;

import com.docsynth.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByOidcIssuerAndOidcSubject(String oidcIssuer, String oidcSubject);
}
