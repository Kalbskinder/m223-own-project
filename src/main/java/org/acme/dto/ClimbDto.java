package org.acme.dto;

import org.acme.enums.ClimbResult;
import org.acme.model.Climb;
import org.acme.model.ClimbingRoute;

/**
 * API representation of a {@link Climb}.
 * The route is referenced by its id only, e.g. {@code "routeId": 1}.
 * The id is ignored when creating a climb; the database assigns it.
 */
public record ClimbDto(
        Long id,
        Long routeId,
        int attempts,
        ClimbResult result,
        int rating,
        String note) {

    public static ClimbDto from(Climb climb) {
        return new ClimbDto(
                climb.getId(),
                climb.getRoute().getId(),
                climb.getAttempts(),
                climb.getResult(),
                climb.getRating(),
                climb.getNote());
    }

    /**
     * @param route the route loaded from the database for {@link #routeId()}
     */
    public Climb toEntity(ClimbingRoute route) {
        Climb climb = new Climb();
        climb.setRoute(route);
        climb.setAttempts(attempts);
        climb.setResult(result);
        climb.setRating(rating);
        climb.setNote(note);
        return climb;
    }
}
