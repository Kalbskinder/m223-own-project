package org.acme.service;

import org.acme.dto.BankAccountDto;
import org.acme.model.BankAccount;
import org.acme.repository.BankAccountRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped 
public class BankService {

    @Inject 
    private BankAccountRepository bankAccountRepository;

    public double getBalance(Long userId) {
        BankAccount account = getBankAccount(userId);
        return account.getMoney();
    }   

    public BankAccount getBankAccount(Long userId) {
        return bankAccountRepository.find("accountOwner.id", userId)
            .firstResultOptional()
            .orElseThrow(() -> new NotFoundException("No bank account for this user"));
    }

    @Transactional 
    public BankAccountDto withdraw(Long userId, double amount) {
        var bal = getBalance(userId);
        withDrawCheck(bal, amount);

        var account = getBankAccount(userId);
        account.setMoney(bal - amount);

        return BankAccountDto.from(account);
    }

    @Transactional 
    public BankAccountDto transfer(Long ownerId, Long targetId, double amount) {
        if (targetId == ownerId) throw new BadRequestException("Cannot transfer money to the same account");
        var ownerBankAccount = getBankAccount(ownerId);
        var targetBankAccount = getBankAccount(targetId);
        withDrawCheck(ownerBankAccount.getMoney(), amount);

        ownerBankAccount.setMoney(ownerBankAccount.getMoney() - amount);
        targetBankAccount.setMoney(targetBankAccount.getMoney() + amount);
        return BankAccountDto.from(ownerBankAccount);
    }

    private void withDrawCheck(double totalMoney, double withdrawAmount) {
        if (withdrawAmount <= 0) throw new BadRequestException("Amount must be positive");
        if ((totalMoney - withdrawAmount) < 0) throw new BadRequestException("Not enough money");

    }
}
