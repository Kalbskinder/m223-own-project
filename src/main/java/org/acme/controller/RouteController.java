package org.acme.controller;

import java.net.URI;
import java.util.List;

import org.acme.dto.ClimbingRouteDto;
import org.acme.security.CurrentUser;
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

@Path("/routes")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RouteController {
    @Inject private ClimbingService climbingService;
    @Inject private CurrentUser currentUser;

    @GET
    public List<ClimbingRouteDto> getAll() {
        return climbingService.getAllRoutes();
    }

    @GET
    @Path("/{id}")
    public ClimbingRouteDto get(@PathParam("id") Long id) {
        return climbingService.getRoute(id);
    }

    @POST
    public Response create(@Valid ClimbingRouteDto dto) {
        ClimbingRouteDto created = climbingService.createRoute(dto, currentUser.id());
        return Response.created(URI.create("/routes/" + created.id())).entity(created).build();
    }

    /** 403 if the route was created by another user. */
    @PUT
    @Path("/{id}")
    public ClimbingRouteDto update(@PathParam("id") Long id, @Valid ClimbingRouteDto dto) {
        return climbingService.updateRoute(id, dto, currentUser.id());
    }
}
