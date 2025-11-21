package com.a3solutions.fsm.auth;

import com.a3solutions.fsm.exceptions.BadRequestException;
import com.a3solutions.fsm.security.JwtService;
import com.a3solutions.fsm.security.Role;
import jakarta.transaction.Transactional;

import org.springframework.http.ResponseEntity;
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
        var accessToken = jwtService.generateAccessToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(userDetails);// later: different expiry

        return new AuthResponse(accessToken, refreshToken, user.getRole().name());
    }

    public ResponseEntity<String> createAdmin() {
        var hashed = passwordEncoder.encode("admin123");

        var user = UserEntity.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@a3fsm.com")
                .password(hashed)
                .role(Role.ADMIN)
                .active(true)
                .build();

        userRepo.save(user);

        return ResponseEntity.ok("Admin created");
    }


    public AuthResponse login(LoginRequest request) {
        // 1) Load user by email
        var user = userRepo.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // 2) Check password manually with PasswordEncoder
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        // 3) Build UserDetails + tokens
        var userDetails = new UserDetailsImpl(user);
        var accessToken = jwtService.generateAccessToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(userDetails);



        return new AuthResponse(accessToken, refreshToken, user.getRole().name());
    }


    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refresh = request.refreshToken();

        // Must be a valid refresh token
        if (!jwtService.isRefreshToken(refresh)) {
            throw new BadRequestException("Not a refresh token");
        }

        String email = jwtService.extractUsername(refresh);
        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        var userDetails = new UserDetailsImpl(user);

        if (!jwtService.isTokenValid(refresh, userDetails)) {
            throw new BadRequestException("Expired or invalid refresh token");
        }

        // Generate new tokens
        var accessToken = jwtService.generateAccessToken(userDetails);
        var newRefreshToken = jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(accessToken, newRefreshToken, user.getRole().name());
    }

}
