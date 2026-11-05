package org.acme.controller;

import org.acme.dto.LoginResponseDto;
import org.acme.dto.UserCredentialsDto;
import org.acme.service.UserService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {
    @Inject private UserService userService;

    /** 200 with a JWT and the user, or 401 if username or password is wrong. */
    @POST
    @Path("/login")
    public LoginResponseDto login(@Valid UserCredentialsDto credentials) {
        return userService.login(credentials);
    }

    /** 201 with the new user, or 409 if the username is taken. */
    @POST
    @Path("/register")
    public Response register(@Valid UserCredentialsDto credentials) {
        return Response.status(Response.Status.CREATED)
                .entity(userService.register(credentials))
                .build();
    }
}
