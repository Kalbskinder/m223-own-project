package org.acme.service;

import org.acme.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped 
public class UserService {
    @Inject private UserRepository userRepository;
}
