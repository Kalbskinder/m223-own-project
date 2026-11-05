package org.acme.dto;

import org.acme.model.Climber;
import org.acme.model.ClimbingGym;
import org.acme.model.ClimbingRoute;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * API representation of a {@link ClimbingRoute}.
 * The gym is referenced by its id only, e.g. {@code "gymId": 1}.
 * {@code id} and {@code ownerId} are only filled in responses. In requests they are ignored:
 * the database assigns the id and the owner is the logged-in user.
 */
public record ClimbingRouteDto(
        Long id,
        @NotNull Long gymId,
        @NotBlank String grade,
        @NotBlank @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "must be a hex color like #ff0000") String color,
        boolean active,
        Long ownerId) {

    public static ClimbingRouteDto from(ClimbingRoute route) {
        return new ClimbingRouteDto(
                route.getId(),
                route.getGym().getId(),
                route.getGrade(),
                route.getColor(),
                route.isActive(),
                route.getOwner().getId());
    }

    /**
     * @param gym   the gym loaded from the database for {@link #gymId()}
     * @param owner the logged-in user
     */
    public ClimbingRoute toEntity(ClimbingGym gym, Climber owner) {
        ClimbingRoute route = new ClimbingRoute();
        route.setGym(gym);
        route.setOwner(owner);
        route.setGrade(grade);
        route.setColor(color);
        route.setActive(active);
        return route;
    }
}
