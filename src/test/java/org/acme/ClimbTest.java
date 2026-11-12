package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.acme.dto.ClimbDto;
import org.acme.enums.ClimbResult;
import org.acme.model.Climb;
import org.acme.model.Climber;
import org.acme.model.ClimbingRoute;
import org.acme.repository.ClimbRepository;
import org.acme.repository.GymRepository;
import org.acme.repository.RouteRepository;
import org.acme.repository.UserRepository;
import org.acme.service.ClimbingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

/**
 * Unit test for the climb logic in {@link ClimbingService}.
 * No Quarkus and no database: the repositories are Mockito mocks.
 */
@ExtendWith(MockitoExtension.class)
class ClimbTest {

    @Mock private ClimbRepository climbRepository;
    @Mock private GymRepository gymRepository;
    @Mock private RouteRepository routeRepository;
    @Mock private UserRepository userRepository;

    /** The real service; Mockito puts the mocks above into its @Inject fields. */
    @InjectMocks private ClimbingService climbingService;

    private Climber owner;
    private Climber otherUser;
    private ClimbingRoute route;

    @BeforeEach
    void setUp() {
        owner = climber(1L, "anna");
        otherUser = climber(2L, "ben");
        route = new ClimbingRoute();
        route.setId(10L);
        route.setGrade("6a+");
        route.setColor("#ff0000");
        route.setActive(true);
        route.setOwner(owner);
    }

    @Test
    void createClimbSetsOwnerAndRoute() {
        when(routeRepository.findByIdOptional(10L)).thenReturn(Optional.of(route));
        when(userRepository.findByIdOptional(1L)).thenReturn(Optional.of(owner));

        doAnswer(call -> {
            call.<Climb>getArgument(0).setId(100L);
            return null;
        }).when(climbRepository).persist(any(Climb.class));

        ClimbDto saved = climbingService.createClimb(newClimb(), owner.getId());

        assertEquals(100L, saved.id());
        assertEquals(owner.getId(), saved.ownerId());
        assertEquals(route.getId(), saved.routeId());
        assertEquals("Crux oben", saved.note());
        verify(climbRepository).persist(any(Climb.class));
    }

    @Test
    void createClimbWithUnknownRouteIsRejected() {
        when(routeRepository.findByIdOptional(10L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> climbingService.createClimb(newClimb(), owner.getId()));
        verify(climbRepository, never()).persist(any(Climb.class));
    }

    @Test
    void ownerCanDeleteClimb() {
        Climb climb = existingClimb(100L, owner);
        when(climbRepository.findByIdOptional(100L)).thenReturn(Optional.of(climb));

        climbingService.deleteClimb(100L, owner.getId());

        verify(climbRepository).delete(climb);
    }

    @Test
    void otherUserCannotDeleteClimb() {
        Climb climb = existingClimb(100L, owner);
        when(climbRepository.findByIdOptional(100L)).thenReturn(Optional.of(climb));

        assertThrows(ForbiddenException.class, () -> climbingService.deleteClimb(100L, otherUser.getId()));
        verify(climbRepository, never()).delete(any(Climb.class));
    }

    @Test
    void otherUserCannotUpdateClimb() {
        Climb climb = existingClimb(100L, owner);
        when(climbRepository.findByIdOptional(100L)).thenReturn(Optional.of(climb));

        assertThrows(ForbiddenException.class,
                () -> climbingService.updateClimb(100L, newClimb(), otherUser.getId()));
        assertEquals("Original", climb.getNote()); // unchanged
    }

    @Test
    void getMissingClimbIsNotFound() {
        when(climbRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> climbingService.getClimb(99L));
    }

    // ---------- Test data ----------

    private ClimbDto newClimb() {
        return new ClimbDto(null, route.getId(), 3, ClimbResult.FLASH, 4, "Crux oben", null);
    }

    private Climb existingClimb(Long id, Climber climbOwner) {
        Climb climb = new Climb();
        climb.setId(id);
        climb.setRoute(route);
        climb.setOwner(climbOwner);
        climb.setAttempts(2);
        climb.setResult(ClimbResult.REPEAT);
        climb.setRating(3);
        climb.setNote("Original");
        return climb;
    }

    private static Climber climber(Long id, String username) {
        Climber climber = new Climber();
        climber.setId(id);
        climber.setUsername(username);
        climber.setPassword("hash");
        return climber;
    }
}
