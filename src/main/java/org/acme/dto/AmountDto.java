package org.acme.dto;

import jakarta.validation.constraints.Positive;

/** Request body for {@code POST /bank/withdraw}. */
public record AmountDto(@Positive double amount) {
}
