package org.acme.dto;

import org.acme.model.ClimbingGym;

/**
 * API representation of a {@link ClimbingGym}.
 * The id is ignored when creating a gym; the database assigns it.
 */
public record ClimbingGymDto(
        Long id,
        String name,
        String address,
        String website) {

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
