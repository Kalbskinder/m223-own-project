package org.acme.security;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/**
 * The user who sent the current request, taken from the verified JWT.
 * Only use it in endpoints annotated with {@code @Authenticated}.
 */
@RequestScoped
public class CurrentUser {
    @Inject private JsonWebToken jwt;

    /** The user's database id, stored as the token's subject ("sub") at login. */
    public Long id() {
        return Long.valueOf(jwt.getSubject());
    }
}
