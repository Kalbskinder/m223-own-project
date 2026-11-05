package org.acme.controller;

import java.net.URI;
import java.util.List;

import org.acme.dto.ClimbDto;
import org.acme.security.CurrentUser;
import org.acme.service.ClimbingService;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/climbs")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClimbController {
    @Inject private ClimbingService climbingService;
    @Inject private CurrentUser currentUser;

    @GET
    public List<ClimbDto> getAll() {
        return climbingService.getAllClimbs();
    }

    @GET
    @Path("/{id}")
    public ClimbDto get(@PathParam("id") Long id) {
        return climbingService.getClimb(id);
    }

    @POST
    public Response create(@Valid ClimbDto dto) {
        ClimbDto created = climbingService.createClimb(dto, currentUser.id());
        return Response.created(URI.create("/climbs/" + created.id())).entity(created).build();
    }

    /** 403 if the climb was logged by another user. */
    @PUT
    @Path("/{id}")
    public ClimbDto update(@PathParam("id") Long id, @Valid ClimbDto dto) {
        return climbingService.updateClimb(id, dto, currentUser.id());
    }

    /** 204 on success, 404 if the climb does not exist, 403 if it belongs to another user. */
    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) {
        climbingService.deleteClimb(id, currentUser.id());
    }
}
