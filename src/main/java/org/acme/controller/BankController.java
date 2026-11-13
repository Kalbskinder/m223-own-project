package org.acme.controller;

import org.acme.dto.BankAccountDto;
import org.acme.security.CurrentUser;
import org.acme.service.BankService;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/bank")
@Authenticated 
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BankController {
    
    @Inject 
    private CurrentUser currentUser;

    @Inject 
    private BankService bankService;

    @Path("/balance")
    public double balance() {
        return bankService.getBalance(currentUser.id());
    }

    @Path("/withdraw") 
    public BankAccountDto withdraw(@PathParam("amount") String amount) {
        return bankService.withdraw(currentUser.id(), Double.parseDouble(amount));
    }

    @Path("/transfer")
    public BankAccountDto transfer(
        @PathParam("amount") double amount,
        @PathParam("to") Long userId
    ) {
        return bankService.transfer(currentUser.id(), userId, amount);
    }

}
