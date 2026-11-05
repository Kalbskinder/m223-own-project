package org.acme.dto;

import org.acme.model.ClimbingRoute;

/**
 * API representation of a {@link ClimbingRoute}.
 * The id is ignored when creating a route; the database assigns it.
 */
public record ClimbingRouteDto(
        Long id,
        String grade,
        String color) {

    public static ClimbingRouteDto from(ClimbingRoute route) {
        return new ClimbingRouteDto(route.getId(), route.getGrade(), route.getColor());
    }

    public ClimbingRoute toEntity() {
        ClimbingRoute route = new ClimbingRoute();
        route.setGrade(grade);
        route.setColor(color);
        return route;
    }
}
