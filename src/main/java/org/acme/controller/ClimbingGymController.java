package org.acme.controller;

import java.net.URI;
import java.util.List;

import org.acme.dto.ClimbingGymDto;
import org.acme.service.ClimbingService;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/gyms")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClimbingGymController {
    @Inject private ClimbingService climbingService;

    @GET
    public List<ClimbingGymDto> getAll() {
        return climbingService.getAllGyms();
    }

    @GET
    @Path("/{id}")
    public ClimbingGymDto get(@PathParam("id") Long id) {
        return climbingService.getGym(id);
    }

    @POST
    public Response create(@Valid ClimbingGymDto dto) {
        ClimbingGymDto created = climbingService.createGym(dto);
        return Response.created(URI.create("/gyms/" + created.id())).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public ClimbingGymDto update(@PathParam("id") Long id, @Valid ClimbingGymDto dto) {
        return climbingService.updateGym(id, dto);
    }
}
