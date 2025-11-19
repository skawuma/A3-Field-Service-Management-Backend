package com.a3solutions.fsm.auth;

import com.a3solutions.fsm.exceptions.BadRequestException;
import com.a3solutions.fsm.security.JwtService;
import com.a3solutions.fsm.security.Role;
import jakarta.transaction.Transactional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.auth
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */

@Service
public class AuthService {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;

    public AuthService(UserRepository userRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authManager) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authManager = authManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepo.existsByEmail(request.email())) {
            throw new BadRequestException("Email already in use");
        }

        var user = UserEntity.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role() == null ? Role.TECH : request.role())
                .active(true)
                .build();

        userRepo.save(user);

        var userDetails = new UserDetailsImpl(user);
        var accessToken = jwtService.generateToken(userDetails);
        var refreshToken = jwtService.generateToken(userDetails); // later: different expiry

        return new AuthResponse(accessToken, refreshToken, user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(
                request.email(), request.password());
        authManager.authenticate(authToken);

        var user = userRepo.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        var userDetails = new UserDetailsImpl(user);
        var accessToken = jwtService.generateToken(userDetails);
        var refreshToken = jwtService.generateToken(userDetails);

        return new AuthResponse(accessToken, refreshToken, user.getRole().name());
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // For now, trust the refresh token as a normal JWT
        String email = jwtService.extractUsername(request.refreshToken());
        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        var userDetails = new UserDetailsImpl(user);

        if (!jwtService.isTokenValid(request.refreshToken(), userDetails)) {
            throw new BadRequestException("Invalid refresh token");
        }

        var accessToken = jwtService.generateToken(userDetails);
        var newRefreshToken = jwtService.generateToken(userDetails);

        return new AuthResponse(accessToken, newRefreshToken, user.getRole().name());
    }
}
