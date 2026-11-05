package org.acme.dto;

import org.acme.model.Climber;

/**
 * Request body for register and login. Only used as input, never sent back to the client.
 */
public record UserCredentialsDto(
        String username,
        String password) {

    /**
     * @param passwordHash the already hashed password; never store {@link #password()} as is
     */
    public Climber toEntity(String passwordHash) {
        Climber user = new Climber();
        user.setUsername(username);
        user.setPassword(passwordHash);
        return user;
    }
}
