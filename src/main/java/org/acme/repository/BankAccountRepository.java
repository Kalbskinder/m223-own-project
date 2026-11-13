package org.acme.repository;

import org.acme.model.BankAccount;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped 
public class BankAccountRepository implements PanacheRepository<BankAccount> {
}
