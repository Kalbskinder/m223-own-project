package org.acme.service;

import org.acme.repository.ClimbRepository;
import org.acme.repository.GymRepository;
import org.acme.repository.RouteRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClimbingService {
    @Inject private ClimbRepository climbRepository;
    @Inject private GymRepository gymRepository;
    @Inject private RouteRepository routeRepository;

    
}
