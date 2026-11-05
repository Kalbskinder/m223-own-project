package org.acme.dto;

import org.acme.model.ClimbingGym;

import jakarta.validation.constraints.NotBlank;

/**
 * API representation of a {@link ClimbingGym}.
 * The id is ignored when creating a gym; the database assigns it.
 */
public record ClimbingGymDto(
        Long id,
        @NotBlank String name,
        @NotBlank String address,
        @NotBlank String website) {

    public static ClimbingGymDto from(ClimbingGym gym) {
        return new ClimbingGymDto(gym.getId(), gym.getName(), gym.getAddress(), gym.getWebsite());
    }

    public ClimbingGym toEntity() {
        ClimbingGym gym = new ClimbingGym();
        gym.setName(name);
        gym.setAddress(address);
        gym.setWebsite(website);
        return gym;
    }
}
