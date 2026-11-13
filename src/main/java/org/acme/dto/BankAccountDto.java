package org.acme.dto;

import org.acme.model.BankAccount;

public record BankAccountDto(
        Long id,
        double money,
        double interestRate,
        UserDto owner
    ) {

    public static BankAccountDto from(BankAccount account) {
        return new BankAccountDto(
                account.getId(),
                account.getMoney(),
                account.getInterestRate(),
                UserDto.from(account.getAccountOwner()));
    }
}
