package com.a3solutions.fsm.config.demo;

import com.a3solutions.fsm.auth.UserEntity;
import com.a3solutions.fsm.auth.UserRepository;
import com.a3solutions.fsm.security.Role;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class DemoUserSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoDataProperties properties;

    public DemoUserSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            DemoDataProperties properties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    public DemoUsers seed() {
        UserEntity admin = upsertUser(
                "Admin",
                "Demo",
                properties.getAdmin(),
                Role.ADMIN
        );
        UserEntity dispatcher = upsertUser(
                "Dispatcher",
                "Demo",
                properties.getDispatcher(),
                Role.DISPATCH
        );
        UserEntity technician = upsertUser(
                "James",
                "Carter",
                properties.getTechnician(),
                Role.TECH
        );

        return new DemoUsers(admin, dispatcher, technician);
    }

    private UserEntity upsertUser(
            String firstName,
            String lastName,
            DemoDataProperties.DemoAccount account,
            Role role
    ) {
        UserEntity user = userRepository.findByEmail(account.getEmail())
                .orElseGet(UserEntity::new);

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(account.getEmail());
        user.setRole(role);
        user.setActive(true);

        if (user.getPassword() == null
                || !passwordEncoder.matches(account.getPassword(), user.getPassword())) {
            user.setPassword(passwordEncoder.encode(account.getPassword()));
        }

        return userRepository.save(user);
    }

    public record DemoUsers(
            UserEntity admin,
            UserEntity dispatcher,
            UserEntity technician
    ) {
    }
}
