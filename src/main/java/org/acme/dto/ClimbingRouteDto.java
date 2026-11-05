package org.acme.dto;

import org.acme.model.ClimbingGym;
import org.acme.model.ClimbingRoute;

/**
 * API representation of a {@link ClimbingRoute}.
 * The gym is referenced by its id only, e.g. {@code "gymId": 1}.
 * The id is ignored when creating a route; the database assigns it.
 */
public record ClimbingRouteDto(
        Long id,
        Long gymId,
        String grade,
        String color,
        boolean active) {

    public static ClimbingRouteDto from(ClimbingRoute route) {
        return new ClimbingRouteDto(
                route.getId(),
                route.getGym().getId(),
                route.getGrade(),
                route.getColor(),
                route.isActive());
    }

    /**
     * @param gym the gym loaded from the database for {@link #gymId()}
     */
    public ClimbingRoute toEntity(ClimbingGym gym) {
        ClimbingRoute route = new ClimbingRoute();
        route.setGym(gym);
        route.setGrade(grade);
        route.setColor(color);
        route.setActive(active);
        return route;
    }
}
