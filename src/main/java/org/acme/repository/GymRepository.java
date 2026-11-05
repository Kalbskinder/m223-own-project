package org.acme.repository;

import org.acme.model.ClimbingGym;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GymRepository implements PanacheRepository<ClimbingGym> {
    
}
