package org.acme.controller;

import org.acme.dto.AmountDto;
import org.acme.dto.BankAccountDto;
import org.acme.dto.TransferDto;
import org.acme.security.CurrentUser;
import org.acme.service.BankService;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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
    private JsonWebToken jwt;


    @Inject
    private BankService bankService;

    /** The logged-in user's account. */
    @GET
    @Path("/account")
    public BankAccountDto account() {
        return BankAccountDto.from(bankService.getBankAccount(currentUser.id()));
    }

    @GET
    @Path("/balance")
    public double balance() {
        return bankService.getBalance(currentUser.id());
    }

    /** 200 with the updated account, or 400 if the amount is invalid or too high. */
    @POST
    @Path("/withdraw")
    public BankAccountDto withdraw(@Valid AmountDto body) {
        return bankService.withdraw(currentUser.id(), body.amount());
    }

    /** 200 with the sender's updated account, 400 if the amount is invalid, 404 if the receiver doesn't exist. */
    @POST
    @Path("/transfer")
    public BankAccountDto transfer(@Valid TransferDto body) {
        return bankService.transfer(currentUser.id(), body.toUsername(), body.amount());
    }
}
