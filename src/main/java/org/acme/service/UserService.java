package org.acme.service;

import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Set;

import org.acme.dto.LoginResponseDto;
import org.acme.dto.UserCredentialsDto;
import org.acme.dto.UserDto;
import org.acme.model.Climber;
import org.acme.repository.UserRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.util.ModularCrypt;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class UserService {
    @Inject private UserRepository userRepository;

    @ConfigProperty(name = "climbtracker.jwt.issuer")
    String issuer;

    @ConfigProperty(name = "climbtracker.jwt.lifetime")
    Duration tokenLifetime;

    @Transactional
    public UserDto register(UserCredentialsDto credentials) {
        if (userRepository.find("username", credentials.username()).firstResultOptional().isPresent()) {
            throw new ClientErrorException("Username already taken", Response.Status.CONFLICT);
        }
        // Only the BCrypt hash is stored, never the plain password.
        Climber user = credentials.toEntity(BcryptUtil.bcryptHash(credentials.password()));
        userRepository.persist(user);
        return UserDto.from(user);
    }

    public LoginResponseDto login(UserCredentialsDto credentials) {
        Climber user = userRepository.find("username", credentials.username())
                .firstResultOptional()
                // Same error for unknown user and wrong password, so nobody can probe for usernames.
                .filter(u -> passwordMatches(credentials.password(), u.getPassword()))
                .orElseThrow(() -> new NotAuthorizedException("Invalid username or password", "Bearer"));
        return new LoginResponseDto(createToken(user), UserDto.from(user));
    }

    private String createToken(Climber user) {
        return Jwt.issuer(issuer)
                .subject(String.valueOf(user.getId()))   // read back by CurrentUser.id()
                .upn(user.getUsername())
                .groups(Set.of("user"))
                .expiresIn(tokenLifetime)
                .sign();
    }

    private static boolean passwordMatches(String plainPassword, String bcryptHash) {
        try {
            PasswordFactory factory = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT);
            Password stored = factory.translate(ModularCrypt.decode(bcryptHash));
            return factory.verify(stored, plainPassword.toCharArray());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not verify password hash", e);
        }
    }
}
