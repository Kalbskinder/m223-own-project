package org.acme.dto;

import org.acme.model.User;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for register and login. Only used as input, never sent back to the client.
 */
public record UserCredentialsDto(
        @NotBlank String username,
        @NotBlank String password) {

    /**
     * @param passwordHash the already hashed password; never store {@link #password()} as is
     */
    public User toEntity(String passwordHash) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordHash);
        return user;
    }
}
