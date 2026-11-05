package org.acme.dto;

import org.acme.model.Climber;

/**
 * API representation of a {@link Climber} for responses.
 * Never contains the password; use {@link UserCredentialsDto} for register/login input.
 */
public record UserDto(
        Long id,
        String username) {

    public static UserDto from(Climber user) {
        return new UserDto(user.getId(), user.getUsername());
    }
}
