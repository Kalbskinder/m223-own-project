package org.acme.service;

import org.acme.dto.BankAccountDto;
import org.acme.model.BankAccount;
import org.acme.model.User;
import org.acme.repository.BankAccountRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class BankService {

    /** Every new user starts with this amount (CHF). */
    public static final double STARTING_BALANCE = 1000;

    @Inject
    BankAccountRepository bankAccountRepository;

    /** Opens the account for a newly registered user. */
    @Transactional
    public BankAccount openAccount(User owner) {
        BankAccount account = new BankAccount();
        account.setAccountOwner(owner);
        account.setMoney(STARTING_BALANCE);
        account.setInterestRate(0);
        bankAccountRepository.persist(account);
        return account;
    }

    public double getBalance(Long userId) {
        return getBankAccount(userId).getMoney();
    }

    public BankAccount getBankAccount(Long userId) {
        return bankAccountRepository.find("accountOwner.id", userId)
            .firstResultOptional()
            .orElseThrow(() -> new NotFoundException("No bank account for this user"));
    }

    private BankAccount getBankAccountByUsername(String username) {
        return bankAccountRepository.find("accountOwner.username", username)
            .firstResultOptional()
            .orElseThrow(() -> new NotFoundException("No bank account for user " + username));
    }

    @Transactional
    public BankAccountDto withdraw(Long userId, double amount) {
        BankAccount account = getBankAccount(userId);
        withdrawCheck(account.getMoney(), amount);
        account.setMoney(account.getMoney() - amount);
        return BankAccountDto.from(account);
    }

    @Transactional
    public BankAccountDto transfer(Long ownerId, String targetUsername, double amount) {
        BankAccount ownerBankAccount = getBankAccount(ownerId);
        BankAccount targetBankAccount = getBankAccountByUsername(targetUsername);
        // Compare the ids with equals(): == on Long compares object references, not values.
        if (ownerBankAccount.getId().equals(targetBankAccount.getId())) {
            throw new BadRequestException("Cannot transfer money to the same account");
        }
        withdrawCheck(ownerBankAccount.getMoney(), amount);

        ownerBankAccount.setMoney(ownerBankAccount.getMoney() - amount);
        targetBankAccount.setMoney(targetBankAccount.getMoney() + amount);
        return BankAccountDto.from(ownerBankAccount);
    }

    private void withdrawCheck(double totalMoney, double withdrawAmount) {
        if (withdrawAmount <= 0) throw new BadRequestException("Amount must be positive");
        if (totalMoney < withdrawAmount) throw new BadRequestException("Not enough money");
    }
}
