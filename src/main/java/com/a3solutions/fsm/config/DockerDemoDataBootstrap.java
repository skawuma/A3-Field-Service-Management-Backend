package com.a3solutions.fsm.config;

import com.a3solutions.fsm.auth.UserEntity;
import com.a3solutions.fsm.auth.UserRepository;
import com.a3solutions.fsm.security.Role;
import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.technician.TechnicianStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DockerDemoDataBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DockerDemoDataBootstrap.class);

    private final boolean bootstrapDemoData;
    private final UserRepository userRepository;
    private final TechnicianRepository technicianRepository;
    private final PasswordEncoder passwordEncoder;

    public DockerDemoDataBootstrap(
            @Value("${app.bootstrap-demo-data:false}") boolean bootstrapDemoData,
            UserRepository userRepository,
            TechnicianRepository technicianRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.bootstrapDemoData = bootstrapDemoData;
        this.userRepository = userRepository;
        this.technicianRepository = technicianRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!bootstrapDemoData) {
            return;
        }

        if (userRepository.count() > 0) {
            log.info("Skipping Docker demo-data bootstrap because users already exist.");
            return;
        }

        UserEntity admin = createUser("Admin", "User", "admin@a3fsm.com", "admin123", Role.ADMIN);
        UserEntity dispatch = createUser("Dispatch", "User", "dispatch@a3fsm.com", "dispatch123", Role.DISPATCH);
        UserEntity debs = createUser("Deborah", "Katimbo", "debs@a3fsm.com", "debs123", Role.TECH);
        UserEntity sam = createUser("Samuel", "Kawuma", "sam@a3fsm.com", "sam123", Role.TECH);

        createTechnician(debs, "Deborah", "Katimbo", "debs@a3fsm.com");
        createTechnician(sam, "Samuel", "Kawuma", "sam@a3fsm.com");

        log.info(
                "Bootstrapped Docker demo users: {}, {}, {}, {}.",
                admin.getEmail(),
                dispatch.getEmail(),
                debs.getEmail(),
                sam.getEmail()
        );
    }

    private UserEntity createUser(String firstName, String lastName, String email, String password, Role role) {
        UserEntity user = UserEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .active(true)
                .build();
        return userRepository.save(user);
    }

    private void createTechnician(UserEntity user, String firstName, String lastName, String email) {
        TechnicianEntity technician = TechnicianEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .status(TechnicianStatus.ACTIVE)
                .userId(user.getId())
                .build();
        technicianRepository.save(technician);
    }
}
