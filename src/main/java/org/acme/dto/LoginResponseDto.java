package org.acme.dto;

/**
 * Response of {@code POST /auth/login}.
 * Send the token with every further request: {@code Authorization: Bearer <token>}.
 */
public record LoginResponseDto(
        String token,
        UserDto user) {
}
