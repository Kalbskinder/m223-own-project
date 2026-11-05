package org.acme.service;

import java.util.List;

import org.acme.dto.ClimbDto;
import org.acme.dto.ClimbingGymDto;
import org.acme.dto.ClimbingRouteDto;
import org.acme.model.Climb;
import org.acme.model.Climber;
import org.acme.model.ClimbingGym;
import org.acme.model.ClimbingRoute;
import org.acme.repository.ClimbRepository;
import org.acme.repository.GymRepository;
import org.acme.repository.RouteRepository;
import org.acme.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ClimbingService {
    @Inject private ClimbRepository climbRepository;
    @Inject private GymRepository gymRepository;
    @Inject private RouteRepository routeRepository;
    @Inject private UserRepository userRepository;

    // ---------- Gyms ----------

    public List<ClimbingGymDto> getAllGyms() {
        return gymRepository.listAll().stream().map(ClimbingGymDto::from).toList();
    }

    public ClimbingGymDto getGym(Long id) {
        return ClimbingGymDto.from(findGym(id));
    }

    @Transactional
    public ClimbingGymDto createGym(ClimbingGymDto dto) {
        ClimbingGym gym = dto.toEntity();
        gymRepository.persist(gym);
        return ClimbingGymDto.from(gym);
    }

    @Transactional
    public ClimbingGymDto updateGym(Long id, ClimbingGymDto dto) {
        ClimbingGym gym = findGym(id);
        // The entity is managed, so the changes are saved when the transaction ends.
        gym.setName(dto.name());
        gym.setAddress(dto.address());
        gym.setWebsite(dto.website());
        return ClimbingGymDto.from(gym);
    }

    // ---------- Routes ----------

    public List<ClimbingRouteDto> getAllRoutes() {
        return routeRepository.listAll().stream().map(ClimbingRouteDto::from).toList();
    }

    public ClimbingRouteDto getRoute(Long id) {
        return ClimbingRouteDto.from(findRoute(id));
    }

    @Transactional
    public ClimbingRouteDto createRoute(ClimbingRouteDto dto, Long userId) {
        ClimbingRoute route = dto.toEntity(referencedGym(dto.gymId()), findUser(userId));
        routeRepository.persist(route);
        return ClimbingRouteDto.from(route);
    }

    @Transactional
    public ClimbingRouteDto updateRoute(Long id, ClimbingRouteDto dto, Long userId) {
        ClimbingRoute route = findRoute(id);
        requireOwner(route.getOwner(), userId, "route");
        route.setGym(referencedGym(dto.gymId()));
        route.setGrade(dto.grade());
        route.setColor(dto.color());
        route.setActive(dto.active());
        return ClimbingRouteDto.from(route);
    }

    // ---------- Climbs ----------

    public List<ClimbDto> getAllClimbs() {
        return climbRepository.listAll().stream().map(ClimbDto::from).toList();
    }

    public ClimbDto getClimb(Long id) {
        return ClimbDto.from(findClimb(id));
    }

    @Transactional
    public ClimbDto createClimb(ClimbDto dto, Long userId) {
        Climb climb = dto.toEntity(referencedRoute(dto.routeId()), findUser(userId));
        climbRepository.persist(climb);
        return ClimbDto.from(climb);
    }

    @Transactional
    public ClimbDto updateClimb(Long id, ClimbDto dto, Long userId) {
        Climb climb = findClimb(id);
        requireOwner(climb.getOwner(), userId, "climb");
        climb.setRoute(referencedRoute(dto.routeId()));
        climb.setAttempts(dto.attempts());
        climb.setResult(dto.result());
        climb.setRating(dto.rating());
        climb.setNote(dto.note());
        return ClimbDto.from(climb);
    }

    @Transactional
    public void deleteClimb(Long id, Long userId) {
        Climb climb = findClimb(id);
        requireOwner(climb.getOwner(), userId, "climb");
        climbRepository.delete(climb);
    }

    // ---------- Helpers ----------

    /** Only the user who created something may change it; everyone else gets 403. */
    private static void requireOwner(Climber owner, Long userId, String what) {
        if (!owner.getId().equals(userId)) {
            throw new ForbiddenException("Only the creator of this " + what + " can change it");
        }
    }

    /** The token is valid, but its user no longer exists -> treat as not logged in. */
    private Climber findUser(Long userId) {
        return userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new NotAuthorizedException("User no longer exists", "Bearer"));
    }

    /** The resource addressed by the URL does not exist -> 404. */
    private ClimbingGym findGym(Long id) {
        return gymRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Gym " + id + " not found"));
    }

    private ClimbingRoute findRoute(Long id) {
        return routeRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Route " + id + " not found"));
    }

    private Climb findClimb(Long id) {
        return climbRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Climb " + id + " not found"));
    }

    /** An id in the request body points to nothing -> 400, the request itself is wrong. */
    private ClimbingGym referencedGym(Long gymId) {
        return gymRepository.findByIdOptional(gymId)
                .orElseThrow(() -> new BadRequestException("Gym " + gymId + " does not exist"));
    }

    private ClimbingRoute referencedRoute(Long routeId) {
        return routeRepository.findByIdOptional(routeId)
                .orElseThrow(() -> new BadRequestException("Route " + routeId + " does not exist"));
    }
}
