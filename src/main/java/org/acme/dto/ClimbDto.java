package org.acme.dto;

import org.acme.enums.ClimbResult;
import org.acme.model.Climb;
import org.acme.model.Climber;
import org.acme.model.ClimbingRoute;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * API representation of a {@link Climb}.
 * The route is referenced by its id only, e.g. {@code "routeId": 1}.
 * {@code id} and {@code ownerId} are only filled in responses. In requests they are ignored:
 * the database assigns the id and the owner is the logged-in user.
 */
public record ClimbDto(
        Long id,
        @NotNull Long routeId,
        @Positive int attempts,
        @NotNull ClimbResult result,
        int rating,
        @NotNull String note,
        Long ownerId) {

    public static ClimbDto from(Climb climb) {
        return new ClimbDto(
                climb.getId(),
                climb.getRoute().getId(),
                climb.getAttempts(),
                climb.getResult(),
                climb.getRating(),
                climb.getNote(),
                climb.getOwner().getId());
    }

    /**
     * @param route the route loaded from the database for {@link #routeId()}
     * @param owner the logged-in user
     */
    public Climb toEntity(ClimbingRoute route, Climber owner) {
        Climb climb = new Climb();
        climb.setRoute(route);
        climb.setOwner(owner);
        climb.setAttempts(attempts);
        climb.setResult(result);
        climb.setRating(rating);
        climb.setNote(note);
        return climb;
    }
}
