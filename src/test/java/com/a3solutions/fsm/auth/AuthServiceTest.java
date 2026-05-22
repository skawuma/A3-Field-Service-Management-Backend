package com.a3solutions.fsm.auth;

import com.a3solutions.fsm.exceptions.BadRequestException;
import com.a3solutions.fsm.security.JwtService;
import com.a3solutions.fsm.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    @Test
    void registerRejectsElevatedRolesWhenSelfRegistrationIsEnabled() {
        RepositoryState repositoryState = new RepositoryState();
        AuthService authService = authService(repositoryState, true, false, "", "");

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.register(new RegisterRequest(
                        "Deborah",
                        "Katimbo",
                        "debs@a3fsm.com",
                        "debs123",
                        Role.ADMIN
                ))
        );

        assertEquals("Self-service registration can only create technician accounts.", ex.getMessage());
        assertEquals(0, repositoryState.users.size());
    }

    @Test
    void registerCreatesTechnicianWhenRoleIsOmitted() {
        RepositoryState repositoryState = new RepositoryState();
        AuthService authService = authService(repositoryState, true, false, "", "");

        AuthResponse response = authService.register(new RegisterRequest(
                "Deborah",
                "Katimbo",
                "debs@a3fsm.com",
                "debs123",
                null
        ));

        assertEquals(1, repositoryState.users.size());
        assertEquals(Role.TECH, repositoryState.users.getFirst().getRole());
        assertEquals("TECH", response.role());
    }

    @Test
    void createAdminRejectsWhenBootstrapCredentialsAreMissing() {
        RepositoryState repositoryState = new RepositoryState();
        AuthService authService = authService(repositoryState, false, true, "", "");

        BadRequestException ex = assertThrows(BadRequestException.class, authService::createAdmin);

        assertEquals("Bootstrap admin credentials are not configured.", ex.getMessage());
    }

    @Test
    void createAdminRejectsWhenAnAdminAlreadyExists() {
        RepositoryState repositoryState = new RepositoryState();
        repositoryState.users.add(UserEntity.builder()
                .id(99L)
                .firstName("Existing")
                .lastName("Admin")
                .email("existing-admin@a3fsm.com")
                .password("encoded-existing")
                .role(Role.ADMIN)
                .active(true)
                .build());

        AuthService authService = authService(repositoryState, false, true, "bootstrap@a3fsm.com", "secure-pass");

        BadRequestException ex = assertThrows(BadRequestException.class, authService::createAdmin);

        assertEquals("An admin account already exists.", ex.getMessage());
    }

    private AuthService authService(
            RepositoryState repositoryState,
            boolean selfRegistrationEnabled,
            boolean adminBootstrapEnabled,
            String bootstrapAdminEmail,
            String bootstrapAdminPassword
    ) {
        UserRepository userRepository = userRepository(repositoryState);
        PasswordEncoder passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "encoded-" + rawPassword;
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encodedPassword.equals(encode(rawPassword));
            }
        };
        JwtService jwtService = new JwtService(
                "test-secret-key-with-at-least-thirty-two-bytes",
                "",
                3_600_000L,
                604_800_000L
        );
        AuthenticationManager authenticationManager = authentication ->
                { throw new AuthenticationServiceException("Authentication manager is not used in these tests."); };

        return new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                authenticationManager,
                selfRegistrationEnabled,
                adminBootstrapEnabled,
                bootstrapAdminEmail,
                bootstrapAdminPassword
        );
    }

    private UserRepository userRepository(RepositoryState repositoryState) {
        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class[]{UserRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByEmail" -> repositoryState.users.stream()
                            .anyMatch(user -> user.getEmail().equals(args[0]));
                    case "existsByRole" -> repositoryState.users.stream()
                            .anyMatch(user -> user.getRole() == args[0]);
                    case "save" -> {
                        UserEntity user = (UserEntity) args[0];
                        if (user.getId() == null) {
                            user.setId(repositoryState.nextId++);
                        }
                        repositoryState.users.add(user);
                        yield user;
                    }
                    case "findByEmail" -> repositoryState.users.stream()
                            .filter(user -> user.getEmail().equals(args[0]))
                            .findFirst();
                    case "toString" -> "InMemoryUserRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Method not implemented in test double: " + method.getName());
                }
        );
    }

    private static final class RepositoryState {
        private final List<UserEntity> users = new ArrayList<>();
        private long nextId = 1L;
    }
}
