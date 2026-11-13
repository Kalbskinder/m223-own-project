package org.acme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Request body for {@code POST /bank/transfer}. The receiver is named by username, since users don't know each other's ids. */
public record TransferDto(
        @NotBlank String toUsername,
        @Positive double amount) {
}
