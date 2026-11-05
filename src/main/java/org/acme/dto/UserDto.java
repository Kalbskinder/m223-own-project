package org.acme.dto;

import org.acme.model.User;

/**
 * API representation of a {@link User} for responses.
 * Never contains the password; use {@link UserCredentialsDto} for register/login input.
 */
public record UserDto(
        Long id,
        String username) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getUsername());
    }
}
